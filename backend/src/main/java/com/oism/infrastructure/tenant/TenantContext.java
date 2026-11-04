package com.oism.infrastructure.tenant;

import java.util.Optional;
import java.util.UUID;

/** Tenant cua request hien tai (lay tu JWT), luu theo luong xu ly. Phai clear sau moi request. */
public final class TenantContext {

    private static final InheritableThreadLocal<UUID> CURRENT = new InheritableThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
