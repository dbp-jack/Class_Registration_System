package com.course.classregistration.application.course.dto;

import com.course.classregistration.domain.course.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String instructor;
    private int maxCapacity;
    private int currentEnrollmentCount;
    private int remainingCapacity;
    private Long createdBy;
    private LocalDateTime enrollmentStartAt;
    private LocalDateTime enrollmentEndAt;
    private LocalDateTime createdAt;

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getInstructor(),
                course.getMaxCapacity(),
                course.getCurrentEnrollmentCount(),
                course.remainingCapacity(),
                course.getCreatedBy(),
                course.getEnrollmentStartAt(),
                course.getEnrollmentEndAt(),
                course.getCreatedAt()
        );
    }
}
