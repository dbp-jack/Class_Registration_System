package com.course.classregistration.domain.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Enrollment 도메인 리포지토리 인터페이스 (DIP)
 * 구현체는 infrastructure 레이어에 위치
 */
public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(Long id);

    /**
     * 특정 강좌에 특정 사용자의 활성 수강 신청 존재 여부 확인
     * 중복 신청 방지에 사용
     */
    boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentStatus status);

    /**
     * 특정 사용자의 수강 신청 목록 조회 (상태 필터) - 전체 리스트
     */
    List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);

    /**
     * 특정 사용자의 수강 신청 목록 조회 (상태 필터) - 페이지네이션
     */
    Page<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    /**
     * 특정 강좌의 수강 신청 목록 조회 (상태 필터)
     * 수강생 목록 조회에 사용
     */
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    /**
     * 대기열 선두 학생 1명 조회 (비관적 락 + SKIP LOCKED)
     *
     * - ORDER BY created_at ASC, id ASC: 밀리초 단위 동시 등록 시 id(AUTO_INCREMENT)로 공정한 선착순 보장
     * - SKIP LOCKED: 이미 다른 트랜잭션이 처리 중인 행은 건너뜀
     *   → 동시 취소가 발생해도 중복 승급 방지
     */
    Optional<Enrollment> findFirstWaitlistedByCourseId(Long courseId);
}
