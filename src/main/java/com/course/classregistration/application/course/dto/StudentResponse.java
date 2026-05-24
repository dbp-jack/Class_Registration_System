package com.course.classregistration.application.course.dto;

import com.course.classregistration.domain.enrollment.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long userId;
    private LocalDateTime enrolledAt;

    public static StudentResponse from(Enrollment enrollment) {
        return new StudentResponse(
                enrollment.getUserId(),
                enrollment.getCreatedAt()
        );
    }
}
