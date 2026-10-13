package com.oism.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Thực thể đại diện cho người thuê / doanh nghiệp sử dụng hệ thống.
 */
@Entity
@Table(name = "tenants")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Tenant extends BaseEntity {

    // Tên doanh nghiệp hoặc tổ chức
    @Column(name = "name", nullable = false)
    private String name;

    // Đường dẫn định danh duy nhất (slug)
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    // Số điện thoại liên lạc
    @Column(name = "phone")
    private String phone;

    // Email liên lạc
    @Column(name = "email")
    private String email;

    // Địa chỉ trụ sở hoặc chi nhánh
    @Column(name = "address")
    private String address;

    // Trạng thái hoạt động của tenant
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
