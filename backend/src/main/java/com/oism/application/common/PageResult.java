package com.oism.application.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Ket qua phan trang chung cua moi API danh sach: {content, page, size, totalElements, totalPages}. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
