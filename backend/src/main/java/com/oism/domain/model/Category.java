package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Danh muc nhieu cap (FR-PROD-01). Ten duy nhat trong cung cap. */
@Entity
@Table(name = "category")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class Category extends TenantScopedEntity {

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false)
    private String name;

    protected Category() {
    }

    public Category(UUID tenantId, UUID parentId, String name) {
        super(tenantId);
        this.parentId = parentId;
        this.name = name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
