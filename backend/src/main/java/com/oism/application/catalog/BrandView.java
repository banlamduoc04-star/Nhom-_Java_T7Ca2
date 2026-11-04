package com.oism.application.catalog;

import com.oism.domain.model.Brand;

import java.util.UUID;

public record BrandView(UUID id, String name) {

    static BrandView from(Brand b) {
        return new BrandView(b.getId(), b.getName());
    }
}
