package com.oism.application.catalog;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Category;
import com.oism.domain.repository.CategoryRepository;
import com.oism.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** FR-PROD-01: danh muc nhieu cap. Ten duy nhat trong cung cap; khong xoa duoc khi con danh muc con hoac san pham. */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> list(UUID tenantId) {
        return categoryRepository.findByTenantIdOrderByNameAsc(tenantId).stream().map(CategoryView::from).toList();
    }

    @Transactional
    public CategoryView create(UUID tenantId, String name, UUID parentId) {
        String cleanName = name.trim();
        assertParentExists(tenantId, parentId);
        if (categoryRepository.existsByTenantIdAndParentIdAndNameIgnoreCase(tenantId, parentId, cleanName)) {
            throw new BusinessException(ErrorCode.PR40901, "Tên danh mục đã tồn tại trong cùng cấp");
        }
        return CategoryView.from(categoryRepository.save(new Category(tenantId, parentId, cleanName)));
    }

    @Transactional
    public CategoryView update(UUID tenantId, UUID id, String name, UUID parentId) {
        Category category = find(tenantId, id);
        if (parentId != null && parentId.equals(id)) {
            throw new BusinessException(ErrorCode.PR40001, "Danh mục không thể là cha của chính nó");
        }
        assertParentExists(tenantId, parentId);
        assertNoCycle(tenantId, id, parentId);
        String cleanName = name.trim();
        boolean sameSlot = cleanName.equalsIgnoreCase(category.getName())
                && java.util.Objects.equals(parentId, category.getParentId());
        if (!sameSlot && categoryRepository.existsByTenantIdAndParentIdAndNameIgnoreCase(tenantId, parentId, cleanName)) {
            throw new BusinessException(ErrorCode.PR40901, "Tên danh mục đã tồn tại trong cùng cấp");
        }
        category.setName(cleanName);
        category.setParentId(parentId);
        return CategoryView.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Category category = find(tenantId, id);
        if (categoryRepository.existsByTenantIdAndParentId(tenantId, id)
                || productRepository.existsByTenantIdAndCategoryId(tenantId, id)) {
            throw new BusinessException(ErrorCode.PR40902, "Danh mục còn danh mục con hoặc sản phẩm");
        }
        categoryRepository.delete(category);
    }

    private Category find(UUID tenantId, UUID id) {
        return categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy danh mục"));
    }

    private void assertParentExists(UUID tenantId, UUID parentId) {
        if (parentId != null) {
            find(tenantId, parentId);
        }
    }

    /** Di len chuoi cha cua parentId: neu gap chinh id thi se tao vong lap. */
    private void assertNoCycle(UUID tenantId, UUID id, UUID parentId) {
        UUID cursor = parentId;
        while (cursor != null) {
            if (cursor.equals(id)) {
                throw new BusinessException(ErrorCode.PR40001, "Không thể chuyển danh mục vào chính nhánh con của nó");
            }
            cursor = categoryRepository.findByIdAndTenantId(cursor, tenantId).map(Category::getParentId).orElse(null);
        }
    }
}
