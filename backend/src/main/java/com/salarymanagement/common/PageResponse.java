package com.salarymanagement.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, API-owned representation of a page of results. Controllers return
 * this instead of Spring Data's {@link Page} directly, so the JSON shape the
 * client depends on is not coupled to Spring Data's internal serialization.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
