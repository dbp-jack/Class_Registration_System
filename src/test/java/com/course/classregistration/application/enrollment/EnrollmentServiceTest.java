package com.course.classregistration.application.enrollment;

import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.application.enrollment.dto.EnrollmentResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService 단위 테스트")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Course course;
    private EnrollmentRequest request;
    private static final Long COURSE_ID = 1L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        course = Course.create("스프링 부트 입문", "김강사", 30, null, null);
        request = EnrollmentRequest.builder().courseId(COURSE_ID).build();
    }

    // ── enroll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("수강 신청 성공 - 정상 조건에서 수강 신청이 완료된다")
    void enroll_success() {
        // given
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(false);
        given(enrollmentRepository.save(any(Enrollment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request);

        // then
        assertThat(response.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(course.getCurrentEnrollmentCount()).isEqualTo(1); // 카운터 증가 확인
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("수강 신청 실패 - 존재하지 않는 강좌 ID로 신청 시 COURSE_NOT_FOUND 예외")
    void enroll_courseNotFound() {
        // given
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.COURSE_NOT_FOUND));
    }

    @Test
    @DisplayName("수강 신청 실패 - 이미 수강 신청된 강좌에 중복 신청 시 ALREADY_ENROLLED 예외")
    void enroll_alreadyEnrolled() {
        // given
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLLED));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 - 정원이 가득 찬 강좌 신청 시 COURSE_FULL 예외")
    void enroll_courseFull() {
        // given
        Course fullCourse = Course.create("정원 초과 강좌", "박강사", 1, null, null);
        fullCourse.increaseEnrollmentCount(); // 정원 1명 → 이미 1명 신청됨

        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(fullCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.COURSE_FULL));
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("수강 취소 성공 - 본인의 수강 신청을 정상 취소한다")
    void cancel_success() {
        // given
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        course.increaseEnrollmentCount(); // 카운터 1로 세팅

        // when
        enrollmentService.cancel(USER_ID, 1L);

        // then
        assertThat(enrollment.isCancelled()).isTrue();
        assertThat(course.getCurrentEnrollmentCount()).isZero(); // 카운터 감소 확인
    }

    @Test
    @DisplayName("수강 취소 실패 - 존재하지 않는 수강 신청 ID로 취소 시 ENROLLMENT_NOT_FOUND 예외")
    void cancel_enrollmentNotFound() {
        // given
        given(enrollmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enrollmentService.cancel(USER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("수강 취소 실패 - 타인의 수강 신청을 취소하려 하면 NOT_ENROLLED_STUDENT 예외")
    void cancel_notOwner() {
        // given
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        Long anotherUserId = 999L;
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.cancel(anotherUserId, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_ENROLLED_STUDENT));
    }

    // ── getMyEnrollments ──────────────────────────────────────────────────────

    @Test
    @DisplayName("내 수강 신청 목록 조회 - 수강 중인 목록만 반환한다")
    void getMyEnrollments_success() {
        // given
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        given(enrollmentRepository.findByUserIdAndStatus(USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(List.of(enrollment));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));

        // when
        List<EnrollmentResponse> responses = enrollmentService.getMyEnrollments(USER_ID);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCourseTitle()).isEqualTo("스프링 부트 입문");
        assertThat(responses.get(0).getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }
}
