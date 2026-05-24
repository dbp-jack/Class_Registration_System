package com.course.classregistration.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 공통 페이지네이션 응답 래퍼
 *
 * Spring의 Page<T>를 클라이언트에게 직접 노출하지 않고
 * 필요한 페이징 정보만 직렬화하여 일관된 응답 구조 제공.
 */
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;       // 현재 페이지 데이터
    private int page;              // 현재 페이지 번호 (0-based)
    private int size;              // 페이지 크기
    private long totalElements;    // 전체 데이터 수
    private int totalPages;        // 전체 페이지 수
    private boolean hasNext;       // 다음 페이지 존재 여부
    private boolean hasPrevious;   // 이전 페이지 존재 여부

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
