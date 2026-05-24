package com.course.classregistration.infrastructure.enrollment;

import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentStatus status);

    List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);

    Page<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
