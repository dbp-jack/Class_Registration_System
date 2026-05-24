package com.course.classregistration.infrastructure.course;

import com.course.classregistration.domain.course.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA 리포지토리 (infrastructure 레이어)
 *
 * 도메인 레이어의 CourseRepository 인터페이스와 직접 연결되지 않음.
 * CourseRepositoryImpl이 이 인터페이스를 주입받아 CourseRepository를 구현.
 */
public interface CourseJpaRepository extends JpaRepository<Course, Long> {

    /**
     * 비관적 쓰기 락으로 강좌 조회
     * 수강 신청 처리 시 동시 접근을 막아 currentEnrollmentCount의 정합성 보장
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithLock(@Param("id") Long id);
}
