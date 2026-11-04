package com.oism.application.branch;

import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;

import java.util.UUID;

public record BranchView(UUID id, String name, BranchType type, String address, boolean active) {

    static BranchView from(Branch b) {
        return new BranchView(b.getId(), b.getName(), b.getType(), b.getAddress(), b.isActive());
    }
}
