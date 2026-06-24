package com.example.scoremate.global.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        PageInfo page
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }

    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }
}
