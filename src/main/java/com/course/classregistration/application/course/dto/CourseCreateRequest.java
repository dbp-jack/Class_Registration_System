package com.course.classregistration.application.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "강좌명을 입력해주세요.")
    private String title;

    @NotBlank(message = "강사명을 입력해주세요.")
    private String instructor;

    @Min(value = 1, message = "최대 수강 인원은 1명 이상이어야 합니다.")
    private int maxCapacity;

    private LocalDateTime enrollmentStartAt;  // null이면 상시 신청 가능
    private LocalDateTime enrollmentEndAt;
}
