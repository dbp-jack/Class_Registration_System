package com.course.classregistration.concurrency;

import com.course.classregistration.TestcontainersConfiguration;
import com.course.classregistration.application.enrollment.EnrollmentService;
import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
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
 * 정원이 10명인 강좌에 30명이 동시에 신청해도 정확히 10명만 성공해야 한다.
 *
 * [동작 원리]
 * 1. findByIdWithLock() → SELECT ... FOR UPDATE 실행, 해당 행에 락 획득
 * 2. 나머지 29개 요청은 트랜잭션 커밋 시까지 대기
 * 3. 락 해제 후 다음 요청이 최신 currentEnrollmentCount를 확인
 * → 정원 초과 시 COURSE_FULL 예외 발생, 정확히 10명만 저장됨
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
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

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
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.COURSE_FULL) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();                    // 30개 스레드 일제히 시작
        doneLatch.await(30, TimeUnit.SECONDS);     // 최대 30초 대기
        executor.shutdown();

        // then
        // 검증 1: 성공 수 = 정원
        assertThat(successCount.get())
                .as("성공한 수강 신청 수는 정원과 일치해야 한다")
                .isEqualTo(maxCapacity);

        // 검증 2: DB의 currentEnrollmentCount = 정원 (카운터 정합성)
        Course updated = courseRepository.findById(courseId).orElseThrow();
        assertThat(updated.getCurrentEnrollmentCount())
                .as("currentEnrollmentCount가 정원을 초과하면 비관적 락이 동작하지 않은 것")
                .isEqualTo(maxCapacity);

        // 검증 3: 실제 ENROLLED 레코드 수 = 정원 (DB 레코드 정합성)
        int enrolledCount = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED).size();
        assertThat(enrolledCount)
                .as("실제 DB에 저장된 수강 신청 레코드 수는 정원과 일치해야 한다")
                .isEqualTo(maxCapacity);

        // 검증 4: 전체 처리 수 = 30 (성공 + 실패 누락 없음)
        assertThat(successCount.get() + failCount.get())
                .as("모든 요청이 성공 또는 COURSE_FULL 예외로 처리되어야 한다")
                .isEqualTo(concurrentUsers);
    }
}
