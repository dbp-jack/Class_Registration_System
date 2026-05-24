package com.course.classregistration.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Course ──────────────────────────────────────────────────────────────
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강좌를 찾을 수 없습니다."),
    COURSE_FULL(HttpStatus.CONFLICT, "수강 정원이 초과되었습니다."),
    ENROLLMENT_PERIOD_NOT_STARTED(HttpStatus.BAD_REQUEST, "수강 신청 기간이 아닙니다."),
    ENROLLMENT_PERIOD_ENDED(HttpStatus.BAD_REQUEST, "수강 신청 기간이 종료되었습니다."),

    // ── Enrollment ──────────────────────────────────────────────────────────
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 수강 신청된 강좌입니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청 내역을 찾을 수 없습니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "취소 가능 기간이 지났습니다."),
    NOT_ENROLLED_STUDENT(HttpStatus.FORBIDDEN, "수강 신청한 학생이 아닙니다."),

    // ── User ────────────────────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_USER_HEADER(HttpStatus.BAD_REQUEST, "X-User-Id 헤더가 누락되었거나 올바르지 않습니다."),

    // ── Common ──────────────────────────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
