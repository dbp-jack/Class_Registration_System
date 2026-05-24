package com.course.classregistration.application.course;

import com.course.classregistration.application.course.dto.CourseCreateRequest;
import com.course.classregistration.application.course.dto.CourseResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.global.common.PageResponse;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * 강좌 생성
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        Course course = Course.create(
                request.getTitle(),
                request.getInstructor(),
                request.getMaxCapacity(),
                request.getEnrollmentStartAt(),
                request.getEnrollmentEndAt()
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
     * 기본값: page=0, size=10, sort=createdAt DESC
     */
    public PageResponse<CourseResponse> getCourses(Pageable pageable) {
        return PageResponse.from(
                courseRepository.findAll(pageable).map(CourseResponse::from)
        );
    }
}
