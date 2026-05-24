package com.course.classregistration.application.enrollment;

import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.application.enrollment.dto.EnrollmentResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import com.course.classregistration.global.common.PageResponse;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    /**
     * 수강 취소 가능 기간 (application.yaml에서 주입)
     * 코드 변경 없이 정책 변경이 가능하도록 외부 설정값으로 관리.
     */
    @Value("${enrollment.cancel-period-hours:24}")
    private int cancelPeriodHours;

    /**
     * 수강 신청
     *
     * [처리 순서]
     * 1. 비관적 락(PESSIMISTIC_WRITE)으로 강좌 조회 → 동시 접근 차단
     * 2. 수강 신청 기간 검증
     * 3. 중복 신청 검증 (ENROLLED / WAITLISTED 모두 차단)
     * 4-A. 정원 초과 → 대기열(WAITLISTED) 등록 (카운터 변경 없음)
     * 4-B. 정원 여유 → 수강 확정(ENROLLED) + 카운터 증가
     *
     * 트랜잭션이 커밋될 때까지 락을 유지하므로
     * 동시에 N명이 신청해도 currentEnrollmentCount 정합성 보장.
     */
    @Transactional
    public EnrollmentResponse enroll(Long userId, EnrollmentRequest request) {
        Long courseId = request.getCourseId();

        // 1. 비관적 락으로 강좌 조회
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        // 2. 수강 신청 기간 검증
        course.validateEnrollmentPeriod();

        // 3. 중복 신청 검증 — ENROLLED 또는 WAITLISTED 상태 모두 차단
        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.ENROLLED)
                || enrollmentRepository.existsByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.WAITLISTED)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        Enrollment enrollment;
        if (course.isFull()) {
            // 4-A. 정원 초과 → 대기열 등록 (카운터 변경 없음)
            enrollment = Enrollment.createWaitlisted(courseId, userId);
        } else {
            // 4-B. 정원 여유 → 수강 확정
            course.increaseEnrollmentCount();
            enrollment = Enrollment.create(courseId, userId);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(saved, course);
    }

    /**
     * 수강 취소
     *
     * [ENROLLED 취소 순서]
     * 1. 수강 신청 내역 조회
     * 2. 본인 확인
     * 3. 이미 취소된 경우 예외
     * 4. 취소 가능 기간 검증 (신청 후 cancelPeriodHours 시간 이내)
     * 5. 비관적 락으로 강좌 조회 → 카운터 감소
     * 6. 상태를 CANCELLED로 변경
     * 7. 대기열 선두 학생 자동 승급 (SKIP LOCKED 쿼리)
     *
     * [WAITLISTED 취소]
     * - 기간 제한 없이 즉시 취소 (대기 중이므로 카운터 영향 없음)
     * - 취소해도 빈 자리가 생기지 않으므로 승급 불필요
     */
    @Transactional
    public void cancel(Long userId, Long enrollmentId) {
        // 1. 수강 신청 내역 조회
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));

        // 2. 본인 확인
        if (!enrollment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED_STUDENT);
        }

        // 3. 이미 취소된 경우 예외
        if (enrollment.isCancelled()) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        // 4. 대기열 취소 — 기간 제한 없이 즉시 취소
        if (enrollment.isWaitlisted()) {
            enrollment.cancel();
            return;
        }

        // 5. 수강 취소 (ENROLLED) — 취소 가능 기간 검증
        LocalDateTime cancelDeadline = enrollment.getCreatedAt().plusHours(cancelPeriodHours);
        if (LocalDateTime.now().isAfter(cancelDeadline)) {
            throw new BusinessException(ErrorCode.CANCEL_PERIOD_EXPIRED);
        }

        // 6. 비관적 락으로 강좌 조회 → 카운터 감소
        Course course = courseRepository.findByIdWithLock(enrollment.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        course.decreaseEnrollmentCount();

        // 7. 상태 변경 후 대기열 선두 학생 승급
        enrollment.cancel();
        promoteFirstWaitlisted(enrollment.getCourseId(), course);
    }

    /**
     * 대기열 선두 학생 승급
     *
     * SKIP LOCKED 쿼리로 조회하므로 동시 취소가 발생해도
     * 각 트랜잭션이 서로 다른 대기자를 선택 → 중복 승급 방지.
     * 대기열이 비어있으면 아무 작업도 하지 않음.
     */
    private void promoteFirstWaitlisted(Long courseId, Course course) {
        enrollmentRepository.findFirstWaitlistedByCourseId(courseId)
                .ifPresent(waitlisted -> {
                    waitlisted.promote();
                    course.increaseEnrollmentCount();
                });
    }

    /**
     * 내 수강 신청 목록 페이지네이션 조회
     *
     * [N+1 해결]
     * 기존: Enrollment 목록 조회(1) + 각 Enrollment마다 Course 조회(N) = N+1 쿼리
     * 개선: Enrollment 목록 조회(1) + courseId IN 절로 Course 일괄 조회(1) = 2 쿼리
     *
     * Enrollment가 courseId를 Long 타입으로 보유(연관관계 미설정)하므로
     * JOIN FETCH 대신 배치 ID 조회 방식으로 해결.
     */
    public PageResponse<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findByUserIdAndStatus(
                userId, EnrollmentStatus.ENROLLED, pageable);

        // courseId 목록으로 Course 일괄 조회 (단 1번의 IN 쿼리)
        Set<Long> courseIds = enrollments.getContent().stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toSet());

        Map<Long, Course> courseMap = courseRepository.findAllByIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        return PageResponse.from(
                enrollments.map(enrollment -> {
                    Course course = Optional.ofNullable(courseMap.get(enrollment.getCourseId()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
                    return EnrollmentResponse.from(enrollment, course);
                })
        );
    }
}
