package com.course.classregistration.concurrency;

import com.course.classregistration.TestcontainersConfiguration;
import com.course.classregistration.application.enrollment.EnrollmentService;
import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 수강 신청 동시성 테스트
 *
 * [배경]
 * 수강 신청 마감 직전에 N명이 동시에 같은 강좌에 신청하면
 * currentEnrollmentCount가 정원을 초과할 수 있는 Race Condition이 존재한다.
 *
 * [검증 목표]
 * PESSIMISTIC_WRITE 락을 사용한 경우,
 * 정원이 10명인 강좌에 30명이 동시에 신청해도
 * 정확히 10명만 ENROLLED되고 나머지 20명은 WAITLISTED로 등록되어야 한다.
 *
 * [동작 원리]
 * 1. findByIdWithLock() → SELECT ... FOR UPDATE 실행, 해당 행에 락 획득
 * 2. 나머지 29개 요청은 트랜잭션 커밋 시까지 대기
 * 3. 락 해제 후 다음 요청이 최신 currentEnrollmentCount를 확인
 * → 정원 내: ENROLLED + 카운터 증가 / 정원 초과: WAITLISTED (카운터 변경 없음)
 */
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=40")
@Import(TestcontainersConfiguration.class)
@DisplayName("수강 신청 동시성 테스트")
class EnrollmentConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("비관적 락 - 30명 동시 신청 시 정원(10명)을 초과하지 않는다")
    void pessimisticLock_prevents_overEnrollment() throws InterruptedException {
        // given
        int maxCapacity = 10;
        int concurrentUsers = 30;

        Course course = courseRepository.save(
                Course.create("동시성 테스트 강좌", "테스트 강사", maxCapacity, null, null, 1L)
        );
        Long courseId = course.getId();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch startLatch = new CountDownLatch(1);   // 일제히 시작 신호
        CountDownLatch doneLatch = new CountDownLatch(concurrentUsers); // 모든 스레드 완료 대기
        AtomicInteger unexpectedErrorCount = new AtomicInteger();

        // 30명이 동시에 같은 강좌를 신청
        for (int i = 0; i < concurrentUsers; i++) {
            final long userId = 1000L + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                    EnrollmentRequest request = EnrollmentRequest.builder()
                            .courseId(courseId)
                            .build();
                    enrollmentService.enroll(userId, request);
                } catch (Exception e) {
                    // 정원 초과 시 WAITLISTED 등록으로 처리되므로 예외는 발생하지 않아야 함
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();                    // 30개 스레드 일제히 시작
        doneLatch.await(30, TimeUnit.SECONDS);     // 최대 30초 대기
        executor.shutdown();

        // then
        // 검증 1: 예상치 못한 예외 없음
        assertThat(unexpectedErrorCount.get())
                .as("모든 요청은 ENROLLED 또는 WAITLISTED로 처리되어야 하며 예외가 없어야 한다")
                .isZero();

        // 검증 2: DB의 currentEnrollmentCount = 정원 (비관적 락 정합성)
        Course updated = courseRepository.findById(courseId).orElseThrow();
        assertThat(updated.getCurrentEnrollmentCount())
                .as("currentEnrollmentCount가 정원을 초과하면 비관적 락이 동작하지 않은 것")
                .isEqualTo(maxCapacity);

        // 검증 3: ENROLLED 레코드 수 = 정원 (정원 내 확정 인원 정합성)
        int enrolledCount = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED).size();
        assertThat(enrolledCount)
                .as("ENROLLED 수는 정원과 일치해야 한다")
                .isEqualTo(maxCapacity);

        // 검증 4: WAITLISTED 레코드 수 = 전체 - 정원 (대기열 인원 정합성)
        int waitlistedCount = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.WAITLISTED).size();
        assertThat(waitlistedCount)
                .as("WAITLISTED 수는 정원 초과 인원과 일치해야 한다")
                .isEqualTo(concurrentUsers - maxCapacity);

        // 검증 5: 전체 처리 수 = 30 (누락 없음)
        assertThat(enrolledCount + waitlistedCount)
                .as("모든 요청이 ENROLLED 또는 WAITLISTED로 처리되어야 한다")
                .isEqualTo(concurrentUsers);
    }
}
