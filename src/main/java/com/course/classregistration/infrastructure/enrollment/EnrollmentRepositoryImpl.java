package com.course.classregistration.infrastructure.enrollment;

import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        return enrollmentJpaRepository.save(enrollment);
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return enrollmentJpaRepository.findById(id);
    }

    @Override
    public boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentStatus status) {
        return enrollmentJpaRepository.existsByCourseIdAndUserIdAndStatus(courseId, userId, status);
    }

    @Override
    public List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status) {
        return enrollmentJpaRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status) {
        return enrollmentJpaRepository.findByCourseIdAndStatus(courseId, status);
    }
}
