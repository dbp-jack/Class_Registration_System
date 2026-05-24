package com.course.classregistration.application.course;

import com.course.classregistration.application.course.dto.CourseCreateRequest;
import com.course.classregistration.application.course.dto.CourseResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService 단위 테스트")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    // ── createCourse ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("강좌 생성 성공 - 저장된 강좌를 CourseResponse로 반환한다")
    void createCourse_success() {
        // given
        CourseCreateRequest request = CourseCreateRequest.builder()
                .title("스프링 부트 입문")
                .instructor("김강사")
                .maxCapacity(30)
                .build();

        Course savedCourse = Course.create("스프링 부트 입문", "김강사", 30, null, null);
        given(courseRepository.save(any(Course.class))).willReturn(savedCourse);

        // when
        CourseResponse response = courseService.createCourse(request);

        // then
        assertThat(response.getTitle()).isEqualTo("스프링 부트 입문");
        assertThat(response.getInstructor()).isEqualTo("김강사");
        assertThat(response.getMaxCapacity()).isEqualTo(30);
        assertThat(response.getCurrentEnrollmentCount()).isZero();
        assertThat(response.getRemainingCapacity()).isEqualTo(30);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    // ── getCourse ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("강좌 단건 조회 성공 - 존재하는 강좌 ID로 조회하면 CourseResponse를 반환한다")
    void getCourse_success() {
        // given
        Course course = Course.create("JPA 심화", "이강사", 20, null, null);
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        // when
        CourseResponse response = courseService.getCourse(1L);

        // then
        assertThat(response.getTitle()).isEqualTo("JPA 심화");
        assertThat(response.getInstructor()).isEqualTo("이강사");
    }

    @Test
    @DisplayName("강좌 단건 조회 실패 - 존재하지 않는 ID 조회 시 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_notFound() {
        // given
        given(courseRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseService.getCourse(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_FOUND);
                });
    }

    // ── getCourses ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("강좌 목록 조회 성공 - 전체 강좌 목록을 CourseResponse 리스트로 반환한다")
    void getCourses_success() {
        // given
        List<Course> courses = List.of(
                Course.create("강좌A", "강사A", 10, null, null),
                Course.create("강좌B", "강사B", 20, null, null)
        );
        given(courseRepository.findAll()).willReturn(courses);

        // when
        List<CourseResponse> responses = courseService.getCourses();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CourseResponse::getTitle)
                .containsExactly("강좌A", "강좌B");
    }

    @Test
    @DisplayName("강좌 목록 조회 - 강좌가 없으면 빈 리스트를 반환한다")
    void getCourses_empty() {
        // given
        given(courseRepository.findAll()).willReturn(List.of());

        // when
        List<CourseResponse> responses = courseService.getCourses();

        // then
        assertThat(responses).isEmpty();
    }
}
