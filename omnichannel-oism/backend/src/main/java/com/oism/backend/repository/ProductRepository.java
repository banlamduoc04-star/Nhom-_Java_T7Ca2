package com.oism.backend.repository;

import com.oism.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByTenantIdAndDeletedFalse(Long tenantId);

    Optional<Product> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Product> findByCategoryIdAndTenantIdAndDeletedFalse(Long categoryId, Long tenantId);

    List<Product> findByBrandIdAndTenantIdAndDeletedFalse(Long brandId, Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.deleted = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("tenantId") Long tenantId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.deleted = false AND p.category.id = :categoryId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeywordAndCategory(@Param("tenantId") Long tenantId, @Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);
}
