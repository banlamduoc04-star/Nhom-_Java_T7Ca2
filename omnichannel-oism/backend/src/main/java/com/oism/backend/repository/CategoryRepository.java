package com.oism.backend.repository;

import com.oism.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu bảng Category (Danh mục sản phẩm).
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Lấy tất cả danh mục chưa xóa của cửa hàng
    List<Category> findByTenantIdAndDeletedFalse(Long tenantId);

    // Tìm danh mục theo ID trong cửa hàng
    Optional<Category> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    // Tìm danh mục theo tên trong cửa hàng
    List<Category> findByNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String name, Long tenantId);
}
