package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Bang nghiep vu thuoc ve mot cua hang (tenant). Moi entity ke thua can gan
 * {@code @Filter(name = TENANT_FILTER, condition = TENANT_FILTER_CONDITION)}.
 * Bo loc "tenantFilter" duoc khai bao o {@link Tenant} va bat theo tung request (NFR-TENANT-01).
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    public static final String TENANT_FILTER = "tenantFilter";
    public static final String TENANT_ID_PARAM = "tenantId";
    public static final String TENANT_FILTER_CONDITION = "tenant_id = :tenantId";

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    protected TenantScopedEntity() {
    }

    protected TenantScopedEntity(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
