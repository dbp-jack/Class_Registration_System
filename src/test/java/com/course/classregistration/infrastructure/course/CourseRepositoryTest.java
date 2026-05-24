package com.course.classregistration.infrastructure.course;

import com.course.classregistration.TestcontainersConfiguration;
import com.course.classregistration.domain.course.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("CourseJpaRepository 통합 테스트 (Testcontainers)")
class CourseRepositoryTest {

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Test
    @DisplayName("강좌 저장 성공 - 저장 후 ID가 자동 생성된다")
    void save_success() {
        // given
        Course course = Course.create("스프링 부트 입문", "김강사", 30, null, null);

        // when
        Course saved = courseJpaRepository.save(course);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("스프링 부트 입문");
        assertThat(saved.getCurrentEnrollmentCount()).isZero();
    }

    @Test
    @DisplayName("강좌 단건 조회 성공 - 저장된 강좌를 ID로 조회할 수 있다")
    void findById_success() {
        // given
        Course course = Course.create("JPA 심화", "이강사", 20, null, null);
        Course saved = courseJpaRepository.save(course);

        // when
        Optional<Course> found = courseJpaRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("JPA 심화");
        assertThat(found.get().getMaxCapacity()).isEqualTo(20);
    }

    @Test
    @DisplayName("비관적 락 조회 성공 - findByIdWithLock으로 강좌를 조회할 수 있다")
    void findByIdWithLock_success() {
        // given
        Course course = Course.create("동시성 테스트", "박강사", 50, null, null);
        Course saved = courseJpaRepository.save(course);

        // when
        Optional<Course> found = courseJpaRepository.findByIdWithLock(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("동시성 테스트");
    }

    @Test
    @DisplayName("존재하지 않는 강좌 조회 - Optional.empty()를 반환한다")
    void findById_notFound() {
        // when
        Optional<Course> found = courseJpaRepository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("강좌 목록 조회 - 저장된 모든 강좌를 반환한다")
    void findAll_success() {
        // given
        courseJpaRepository.save(Course.create("강좌A", "강사A", 10, null, null));
        courseJpaRepository.save(Course.create("강좌B", "강사B", 20, null, null));

        // when
        var courses = courseJpaRepository.findAll();

        // then
        assertThat(courses).hasSizeGreaterThanOrEqualTo(2);
    }
}
