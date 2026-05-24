package com.course.classregistration.domain.course;

import java.util.List;
import java.util.Optional;

/**
 * Course 도메인 리포지토리 인터페이스
 *
 * DIP 적용: 인터페이스는 domain 레이어에 정의, 구현체는 infrastructure 레이어에 위치.
 * domain → infrastructure 방향의 의존성을 역전시켜 도메인이 JPA에 직접 의존하지 않음.
 */
public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(Long id);

    /**
     * 비관적 락(PESSIMISTIC_WRITE)을 사용해 강좌 조회
     * 수강 신청 처리 시 동시성 문제 방지를 위해 사용
     */
    Optional<Course> findByIdWithLock(Long id);

    List<Course> findAll();
}
