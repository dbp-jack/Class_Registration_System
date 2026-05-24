package com.course.classregistration.presentation.enrollment;

import com.course.classregistration.application.enrollment.EnrollmentService;
import com.course.classregistration.application.enrollment.dto.EnrollmentRequest;
import com.course.classregistration.application.enrollment.dto.EnrollmentResponse;
import com.course.classregistration.global.common.ApiResponse;
import com.course.classregistration.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "수강 신청 API")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 수강 신청
     * POST /api/enrollments
     * Header: X-User-Id (수강 신청자 ID)
     */
    @PostMapping
    @Operation(summary = "수강 신청", description = "X-User-Id 헤더의 사용자로 강좌를 수강 신청합니다.")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("수강 신청이 완료되었습니다.", response));
    }

    /**
     * 수강 취소
     * DELETE /api/enrollments/{enrollmentId}
     * Header: X-User-Id (취소 요청자 ID)
     */
    @DeleteMapping("/{enrollmentId}")
    @Operation(summary = "수강 취소", description = "수강 신청을 취소합니다. 본인만 취소 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId) {
        enrollmentService.cancel(userId, enrollmentId);
        return ResponseEntity.ok(ApiResponse.ok("수강 취소가 완료되었습니다."));
    }

    /**
     * 내 수강 신청 목록 페이지네이션 조회
     * GET /api/enrollments/me?page=0&size=10&sort=createdAt,desc
     * Header: X-User-Id (조회 대상 사용자 ID)
     */
    @GetMapping("/me")
    @Operation(summary = "내 수강 신청 목록", description = "현재 사용자의 수강 신청 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getMyEnrollments(userId, pageable)));
    }
}
