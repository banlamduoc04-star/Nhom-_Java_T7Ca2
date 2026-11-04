package com.oism.backend.repository;

import com.oism.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTenantIdAndDeletedFalse(Long tenantId);

    Optional<Category> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Category> findByNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String name, Long tenantId);

    List<Category> findByParentIdAndTenantIdAndDeletedFalse(Long parentId, Long tenantId);
}
