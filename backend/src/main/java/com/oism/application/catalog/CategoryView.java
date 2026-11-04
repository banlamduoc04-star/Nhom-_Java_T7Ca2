package com.oism.application.catalog;

import com.oism.domain.model.Category;

import java.util.UUID;

public record CategoryView(UUID id, UUID parentId, String name) {

    static CategoryView from(Category c) {
        return new CategoryView(c.getId(), c.getParentId(), c.getName());
    }
}
