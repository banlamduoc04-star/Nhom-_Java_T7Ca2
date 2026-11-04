package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bien the ban duoc (FR-PROD-02..04). avg_cost do module INV cap nhat khi xac nhan phieu nhap (FR-COST-01). */
@Entity
@Table(name = "sku")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class Sku extends TenantScopedEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "code", nullable = false)
    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributes = new HashMap<>();

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "retail_price", nullable = false)
    private BigDecimal retailPrice;

    @Column(name = "wholesale_price")
    private BigDecimal wholesalePrice;

    @Column(name = "avg_cost", nullable = false)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    protected Sku() {
    }

    public Sku(UUID tenantId, UUID productId, String code, Map<String, String> attributes, String barcode,
               BigDecimal retailPrice, BigDecimal wholesalePrice, int reorderPoint) {
        super(tenantId);
        this.productId = productId;
        this.code = code;
        this.attributes = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
        this.barcode = barcode;
        this.retailPrice = retailPrice;
        this.wholesalePrice = wholesalePrice;
        this.reorderPoint = reorderPoint;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public BigDecimal getWholesalePrice() {
        return wholesalePrice;
    }

    /** FR-PROD-04: doi gia khong anh huong don da tao vi don luu unit_price tai thoi diem ban. */
    public void changePrices(BigDecimal retailPrice, BigDecimal wholesalePrice) {
        this.retailPrice = retailPrice;
        this.wholesalePrice = wholesalePrice;
    }

    public BigDecimal getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(BigDecimal avgCost) {
        this.avgCost = avgCost;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public CatalogStatus getStatus() {
        return status;
    }

    public void setStatus(CatalogStatus status) {
        this.status = status;
    }
}
