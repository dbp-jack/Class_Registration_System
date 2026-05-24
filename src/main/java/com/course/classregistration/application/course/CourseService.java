package com.course.classregistration.application.course;

import com.course.classregistration.application.course.dto.CourseCreateRequest;
import com.course.classregistration.application.course.dto.CourseResponse;
import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.course.CourseRepository;
import com.course.classregistration.global.exception.BusinessException;
import com.course.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
     * 강좌 목록 조회
     */
    public List<CourseResponse> getCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::from)
                .collect(Collectors.toList());
    }
}
