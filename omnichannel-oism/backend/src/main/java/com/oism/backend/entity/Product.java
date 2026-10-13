package com.oism.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Thực thể thông tin sản phẩm trong hệ thống.
 */
@Entity
@Table(name = "products")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product extends BaseEntity {

    // Tên sản phẩm
    @Column(name = "name", nullable = false)
    private String name;

    // Mô tả sản phẩm
    @Column(name = "description")
    private String description;

    // Đường dẫn ảnh minh họa sản phẩm
    @Column(name = "image_url")
    private String imageUrl;

    // Danh mục chứa sản phẩm này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Tenant sở hữu sản phẩm này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    // Trạng thái kích hoạt sản phẩm
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
