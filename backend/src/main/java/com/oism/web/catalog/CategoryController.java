package com.oism.web.catalog;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.catalog.CategoryService;
import com.oism.application.catalog.CategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-PROD-01: danh muc nhieu cap. GET: moi vai tro; ghi: Owner. */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public record CategoryRequest(
            @NotBlank(message = "Tên danh mục không được để trống")
            @Size(max = 150, message = "Tên danh mục tối đa 150 ký tự")
            String name,
            UUID parentId) {
    }

    @GetMapping
    @Operation(summary = "Danh sách danh mục (phẳng, có parentId để dựng cây)")
    public List<CategoryView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return categoryService.list(user.tenantId());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Tạo danh mục (Owner); tên duy nhất trong cùng cấp")
    public ResponseEntity<CategoryView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CategoryRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(user.tenantId(), r.name(), r.parentId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Sửa/chuyển danh mục (Owner)")
    public CategoryView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                               @Valid @RequestBody CategoryRequest r) {
        return categoryService.update(user.tenantId(), id, r.name(), r.parentId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Xóa danh mục (Owner); 409 PR40902 nếu còn danh mục con hoặc sản phẩm")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        categoryService.delete(user.tenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
