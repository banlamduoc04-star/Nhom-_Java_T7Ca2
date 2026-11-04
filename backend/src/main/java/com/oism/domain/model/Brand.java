package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Thuong hieu (FR-PROD-01). Ten duy nhat trong tenant. */
@Entity
@Table(name = "brand")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class Brand extends TenantScopedEntity {

    @Column(name = "name", nullable = false)
    private String name;

    protected Brand() {
    }

    public Brand(UUID tenantId, String name) {
        super(tenantId);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
