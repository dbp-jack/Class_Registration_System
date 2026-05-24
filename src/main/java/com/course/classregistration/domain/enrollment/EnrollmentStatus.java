package com.course.classregistration.domain.enrollment;

/**
 * 수강 신청 상태
 *
 * ENROLLED   : 수강 신청 완료 (정원 내 확정)
 * WAITLISTED : 대기열 등록 (정원 초과로 대기 중)
 * CANCELLED  : 수강 취소 완료
 */
public enum EnrollmentStatus {
    ENROLLED,
    WAITLISTED,
    CANCELLED
}
