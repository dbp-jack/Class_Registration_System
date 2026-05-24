package com.course.classregistration.infrastructure.enrollment;

import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentStatus status);

    List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);

    Page<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    /**
     * 대기열 선두 학생 1명 조회 — 비관적 락 + SKIP LOCKED
     *
     * SKIP LOCKED를 사용하는 이유:
     *   동시에 여러 취소가 발생하면 각 트랜잭션이 동일한 대기열 1번 행에 접근한다.
     *   FOR UPDATE 만 쓰면 뒤 트랜잭션이 락 해제를 기다렸다가 같은 행을 또 승급시킨다.
     *   SKIP LOCKED는 이미 잠긴 행을 건너뛰어 다음 대기자를 선택하므로
     *   중복 승급 없이 공정한 선착순을 보장한다.
     *
     * ORDER BY created_at ASC, id ASC:
     *   밀리초 단위로 동시 등록된 경우 id(AUTO_INCREMENT)가 타이브레이커 역할을 한다.
     *
     * H2는 SKIP LOCKED를 지원하지 않으므로 통합 테스트는 Testcontainers(PostgreSQL) 사용.
     */
    @Query(value = """
            SELECT * FROM enrollments
            WHERE course_id = :courseId AND status = 'WAITLISTED'
            ORDER BY created_at ASC, id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Enrollment> findFirstWaitlistedByCourseId(@Param("courseId") Long courseId);
}
