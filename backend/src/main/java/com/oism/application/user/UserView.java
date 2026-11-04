package com.oism.application.user;

import com.oism.domain.model.AppUser;
import com.oism.domain.model.Role;

import java.util.UUID;

public record UserView(UUID id, String email, String phone, String fullName, Role role, UUID branchId, boolean active) {

    static UserView from(AppUser u) {
        return new UserView(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(), u.getRole(),
                u.getBranchId(), u.isActive());
    }
}
