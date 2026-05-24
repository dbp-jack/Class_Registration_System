package com.course.classregistration.application.enrollment.dto;

import com.course.classregistration.domain.course.Course;
import com.course.classregistration.domain.enrollment.Enrollment;
import com.course.classregistration.domain.enrollment.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EnrollmentResponse {

    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String instructor;
    private Long userId;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;

    public static EnrollmentResponse from(Enrollment enrollment, Course course) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                course.getTitle(),
                course.getInstructor(),
                enrollment.getUserId(),
                enrollment.getStatus(),
                enrollment.getCreatedAt()
        );
    }
}
