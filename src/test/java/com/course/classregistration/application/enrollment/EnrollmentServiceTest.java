package com.course.classregistration.application.enrollment;

import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.application.enrollment.dto.EnrollmentResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import com.course.classregistration.global.common.PageResponse;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        course = Course.create("스프링 부트 입문", "김강사", 30, null, null, 1L);
        // 단위 테스트에서는 DB 저장이 없으므로 id를 직접 주입
        ReflectionTestUtils.setField(course, "id", COURSE_ID);
        request = EnrollmentRequest.builder().courseId(COURSE_ID).build();
        // cancelPeriodHours 기본값 주입 (application.yaml 없이 단위 테스트 실행)
        ReflectionTestUtils.setField(enrollmentService, "cancelPeriodHours", 24);
    }

    // ── enroll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("수강 신청 성공 - 정상 조건에서 수강 신청이 완료된다")
    void enroll_success() {
        // given
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(false);
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.WAITLISTED))
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
    @DisplayName("수강 신청 실패 - 이미 대기열에 있는 강좌 재신청 시 ALREADY_ENROLLED 예외")
    void enroll_alreadyWaitlisted() {
        // given
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(false);
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.WAITLISTED))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLLED));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 - 정원이 가득 찬 강좌 신청 시 대기열(WAITLISTED)에 등록된다")
    void enroll_waitlisted_whenFull() {
        // given
        Course fullCourse = Course.create("정원 초과 강좌", "박강사", 1, null, null, 1L);
        fullCourse.increaseEnrollmentCount(); // 정원 1명 → 이미 1명 신청됨

        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(fullCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.ENROLLED))
                .willReturn(false);
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, EnrollmentStatus.WAITLISTED))
                .willReturn(false);
        given(enrollmentRepository.save(any(Enrollment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        EnrollmentResponse response = enrollmentService.enroll(USER_ID, request);

        // then
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(fullCourse.getCurrentEnrollmentCount()).isEqualTo(1); // 카운터 변경 없음
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("수강 취소 성공 - 취소 가능 기간(24h) 이내에 본인이 취소하면 성공한다")
    void cancel_success() {
        // given
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        // createdAt을 1시간 전으로 설정 (24시간 이내 → 취소 가능)
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now().minusHours(1));

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.findFirstWaitlistedByCourseId(COURSE_ID)).willReturn(Optional.empty());
        course.increaseEnrollmentCount();

        // when
        enrollmentService.cancel(USER_ID, 1L);

        // then
        assertThat(enrollment.isCancelled()).isTrue();
        assertThat(course.getCurrentEnrollmentCount()).isZero();
    }

    @Test
    @DisplayName("수강 취소 성공 - ENROLLED 취소 시 대기열 첫 번째 학생이 자동 승급된다")
    void cancel_enrolled_promotesWaitlisted() {
        // given
        course.increaseEnrollmentCount(); // 현재 1명 수강 중

        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now().minusHours(1));

        Enrollment waitlisted = Enrollment.createWaitlisted(COURSE_ID, 200L);

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.findFirstWaitlistedByCourseId(COURSE_ID)).willReturn(Optional.of(waitlisted));

        // when
        enrollmentService.cancel(USER_ID, 1L);

        // then
        assertThat(enrollment.isCancelled()).isTrue();
        assertThat(waitlisted.isEnrolled()).isTrue();          // 대기열 → ENROLLED 승급
        assertThat(course.getCurrentEnrollmentCount()).isEqualTo(1); // 감소 후 승급 → 원상복구
    }

    @Test
    @DisplayName("수강 취소 성공 - ENROLLED 취소 시 대기열이 없으면 승급 없이 카운터만 감소한다")
    void cancel_enrolled_noWaitlist() {
        // given
        course.increaseEnrollmentCount();

        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now().minusHours(1));

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(courseRepository.findByIdWithLock(COURSE_ID)).willReturn(Optional.of(course));
        given(enrollmentRepository.findFirstWaitlistedByCourseId(COURSE_ID)).willReturn(Optional.empty());

        // when
        enrollmentService.cancel(USER_ID, 1L);

        // then
        assertThat(enrollment.isCancelled()).isTrue();
        assertThat(course.getCurrentEnrollmentCount()).isZero(); // 승급 없으므로 0
    }

    @Test
    @DisplayName("대기열 취소 성공 - WAITLISTED 취소는 기간 제한 없이 즉시 취소된다")
    void cancel_waitlisted_success() {
        // given
        // createdAt을 25시간 전으로 설정 (ENROLLED라면 취소 불가 기간)
        Enrollment waitlisted = Enrollment.createWaitlisted(COURSE_ID, USER_ID);
        ReflectionTestUtils.setField(waitlisted, "createdAt", LocalDateTime.now().minusHours(25));

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(waitlisted));

        // when
        enrollmentService.cancel(USER_ID, 1L);

        // then
        assertThat(waitlisted.isCancelled()).isTrue();
        // 대기열은 카운터에 영향이 없으므로 강좌 조회도 하지 않아야 함
        verify(courseRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("수강 취소 실패 - 취소 가능 기간(24h) 초과 시 CANCEL_PERIOD_EXPIRED 예외")
    void cancel_periodExpired() {
        // given
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        // createdAt을 25시간 전으로 설정 (24시간 초과 → 취소 불가)
        ReflectionTestUtils.setField(enrollment, "createdAt", LocalDateTime.now().minusHours(25));

        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.cancel(USER_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CANCEL_PERIOD_EXPIRED));

        // 기간 만료 시 강좌 카운터는 변경되지 않아야 함
        verify(courseRepository, never()).findByIdWithLock(any());
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
    @DisplayName("내 수강 신청 목록 조회 - courseId 배치 조회로 N+1 없이 PageResponse를 반환한다")
    void getMyEnrollments_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Enrollment enrollment = Enrollment.create(COURSE_ID, USER_ID);
        given(enrollmentRepository.findByUserIdAndStatus(USER_ID, EnrollmentStatus.ENROLLED, pageable))
                .willReturn(new PageImpl<>(List.of(enrollment), pageable, 1));
        // 기존: findById(COURSE_ID) → N번 호출
        // 개선: findAllByIds(Set) → 1번 호출
        given(courseRepository.findAllByIds(Set.of(COURSE_ID))).willReturn(List.of(course));

        // when
        PageResponse<EnrollmentResponse> response = enrollmentService.getMyEnrollments(USER_ID, pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getCourseTitle()).isEqualTo("스프링 부트 입문");
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        // findById가 호출되지 않음을 검증 (N+1 제거 확인)
        verify(courseRepository, never()).findById(any());
    }
}
