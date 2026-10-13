package com.oism.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Thực thể danh mục phân loại sản phẩm.
 */
@Entity
@Table(name = "categories")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Category extends BaseEntity {

    // Tên danh mục sản phẩm
    @Column(name = "name", nullable = false)
    private String name;

    // Mô tả chi tiết danh mục
    @Column(name = "description")
    private String description;

    // Tenant sở hữu danh mục này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}
