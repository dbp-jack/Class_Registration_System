package com.course.classregistration.application.course;

import com.course.classregistration.application.course.dto.CourseCreateRequest;
import com.course.classregistration.application.course.dto.CourseResponse;
import com.course.classregistration.application.course.dto.StudentResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.domain.enrollment.EnrollmentRepository;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import com.course.classregistration.global.common.PageResponse;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * 강좌 생성
     * createdBy는 Request Body가 아닌 X-User-Id 헤더에서 주입받아 서버에서 설정
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request, Long createdBy) {
        Course course = Course.create(
                request.getTitle(),
                request.getInstructor(),
                request.getMaxCapacity(),
                request.getEnrollmentStartAt(),
                request.getEnrollmentEndAt(),
                createdBy
        );
        return CourseResponse.from(courseRepository.save(course));
    }

    /**
     * 강좌 단건 조회
     */
    public CourseResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        return CourseResponse.from(course);
    }

    /**
     * 강좌 목록 페이지네이션 조회
     */
    public PageResponse<CourseResponse> getCourses(Pageable pageable) {
        return PageResponse.from(
                courseRepository.findAll(pageable).map(CourseResponse::from)
        );
    }

    /**
     * 수강생 목록 조회 (개설자 전용)
     * 요청자 userId와 course.createdBy가 일치할 때만 조회 허용
     */
    public List<StudentResponse> getStudents(Long courseId, Long requestUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.isCreatedBy(requestUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }

        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED)
                .stream()
                .map(StudentResponse::from)
                .collect(Collectors.toList());
    }
}
