package com.oism.application.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Quy uoc phan trang: page tinh tu 0, size mac dinh 20, toi da 100. */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, sort);
    }
}
