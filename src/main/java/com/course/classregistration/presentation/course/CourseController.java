package com.course.classregistration.presentation.course;

import com.course.classregistration.application.course.CourseService;
import com.course.classregistration.application.course.dto.CourseCreateRequest;
import com.course.classregistration.application.course.dto.CourseResponse;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "강좌 관리 API")
public class CourseController {

    private final CourseService courseService;

    /**
     * 강좌 생성
     * POST /api/courses
     */
    @PostMapping
    @Operation(summary = "강좌 생성", description = "새로운 강좌를 등록합니다.")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseCreateRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("강좌가 생성되었습니다.", response));
    }

    /**
     * 강좌 단건 조회
     * GET /api/courses/{courseId}
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "강좌 단건 조회", description = "강좌 ID로 강좌 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourse(courseId)));
    }

    /**
     * 강좌 목록 페이지네이션 조회
     * GET /api/courses?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    @Operation(summary = "강좌 목록 조회", description = "강좌 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getCourses(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourses(pageable)));
    }
}
