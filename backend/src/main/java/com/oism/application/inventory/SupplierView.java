package com.oism.application.inventory;

import com.oism.domain.model.Supplier;

import java.util.UUID;

public record SupplierView(UUID id, String name, String phone, String address, boolean active) {

    static SupplierView from(Supplier s) {
        return new SupplierView(s.getId(), s.getName(), s.getPhone(), s.getAddress(), s.isActive());
    }
}
