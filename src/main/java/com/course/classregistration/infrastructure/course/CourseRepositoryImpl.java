package com.course.classregistration.infrastructure.course;

import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * CourseRepository 구현체 (infrastructure 레이어)
 *
 * domain 레이어의 CourseRepository 인터페이스를 구현.
 * CourseJpaRepository에 위임(delegation)하여 실제 DB 접근 처리.
 */
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;

    @Override
    public Course save(Course course) {
        return courseJpaRepository.save(course);
    }

    @Override
    public Optional<Course> findById(Long id) {
        return courseJpaRepository.findById(id);
    }

    @Override
    public Optional<Course> findByIdWithLock(Long id) {
        return courseJpaRepository.findByIdWithLock(id);
    }

    @Override
    public Page<Course> findAll(Pageable pageable) {
        return courseJpaRepository.findAll(pageable);
    }
}
