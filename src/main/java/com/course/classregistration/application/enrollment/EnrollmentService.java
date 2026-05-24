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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    /**
     * 수강 신청
     *
     * [처리 순서]
     * 1. 비관적 락(PESSIMISTIC_WRITE)으로 강좌 조회 → 동시 접근 차단
     * 2. 수강 신청 기간 검증
     * 3. 중복 신청 검증
     * 4. 정원 확인 및 카운터 증가
     * 5. Enrollment 저장
     *
     * 트랜잭션이 커밋될 때까지 락을 유지하므로
     * 동시에 N명이 신청해도 currentEnrollmentCount 정합성 보장
     */
    @Transactional
    public EnrollmentResponse enroll(Long userId, EnrollmentRequest request) {
        Long courseId = request.getCourseId();

        // 1. 비관적 락으로 강좌 조회
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        // 2. 수강 신청 기간 검증
        course.validateEnrollmentPeriod();

        // 3. 중복 신청 검증
        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.ENROLLED)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        // 4. 정원 확인 및 카운터 증가 (정원 초과 시 COURSE_FULL 예외)
        course.increaseEnrollmentCount();

        // 5. 수강 신청 저장
        Enrollment enrollment = Enrollment.create(courseId, userId);
        Enrollment saved = enrollmentRepository.save(enrollment);

        return EnrollmentResponse.from(saved, course);
    }

    /**
     * 수강 취소
     *
     * [처리 순서]
     * 1. 수강 신청 내역 조회
     * 2. 본인 확인
     * 3. 활성 상태 확인 (이미 취소된 경우 예외)
     * 4. 비관적 락으로 강좌 조회 → 카운터 감소
     * 5. 상태를 CANCELLED로 변경
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

        // 3. 활성 상태 확인
        if (enrollment.isCancelled()) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        // 4. 비관적 락으로 강좌 조회 → 카운터 감소
        Course course = courseRepository.findByIdWithLock(enrollment.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        course.decreaseEnrollmentCount();

        // 5. 상태 변경
        enrollment.cancel();
    }

    /**
     * 내 수강 신청 목록 페이지네이션 조회
     *
     * TODO: N+1 문제 존재 - 각 Enrollment마다 Course 조회 쿼리 발생
     *       Phase 6 리팩토링에서 JPQL fetch join으로 개선 예정
     */
    public PageResponse<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        return PageResponse.from(
                enrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ENROLLED, pageable)
                        .map(enrollment -> {
                            Course course = courseRepository.findById(enrollment.getCourseId())
                                    .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
                            return EnrollmentResponse.from(enrollment, course);
                        })
        );
    }
}
