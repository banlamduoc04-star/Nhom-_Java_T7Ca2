package com.oism.infrastructure.seed;

import com.oism.application.auth.AuthService;
import com.oism.application.auth.RegisterCommand;
import com.oism.application.auth.RegisterResult;
import com.oism.application.branch.BranchService;
import com.oism.application.catalog.BrandService;
import com.oism.application.catalog.BrandView;
import com.oism.application.catalog.CategoryService;
import com.oism.application.catalog.CategoryView;
import com.oism.application.catalog.ProductService;
import com.oism.application.catalog.ProductView;
import com.oism.application.catalog.SkuInput;
import com.oism.application.catalog.SkuView;
import com.oism.application.inventory.PurchaseReceiptService;
import com.oism.application.inventory.ReceiptLine;
import com.oism.application.inventory.ReceiptView;
import com.oism.application.inventory.SupplierService;
import com.oism.application.inventory.SupplierView;
import com.oism.application.user.UserService;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.Role;
import com.oism.domain.repository.AppUserRepository;
import com.oism.domain.service.Ean13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Du lieu mau cho demo/dev (Sprint 2): 2 tenant, chi nhanh, danh muc, 50 SKU, nha cung cap, tai khoan moi vai tro,
 * phieu nhap da xac nhan (co ton kho + gia von). Chi chay khi oism.seed.enabled=true; chay lai khong tao trung.
 * Mat khau mau chi dung cho dev/demo, khong dung o moi truong that.
 */
@Component
@ConditionalOnProperty(name = "oism.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    static final String PASSWORD = "Oism@12345";
    static final String OWNER_A_EMAIL = "owner.a@oism.vn";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] PRODUCT_NAMES = {
            "Ao Polo Nam", "Ao Thun Nu", "Quan Jean Nam", "Quan Short Nu", "Ao Khoac Gio",
            "Vay Cong So", "Ao So Mi Nam", "Quan Tay Nu", "Non Luoi Trai", "Tui Deo Cheo"
    };
    private static final String[] PRODUCT_CODES = {
            "POLO", "THUN", "JEAN", "SHORT", "KHOAC", "VAY", "SOMI", "TAY", "NON", "TUI"
    };
    private static final String[] SIZES = {"S", "M", "L", "XL", "XXL"};

    private final AppUserRepository appUserRepository;
    private final AuthService authService;
    private final BranchService branchService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final PurchaseReceiptService receiptService;

    public DemoDataSeeder(AppUserRepository appUserRepository, AuthService authService, BranchService branchService,
                          UserService userService, CategoryService categoryService, BrandService brandService,
                          ProductService productService, SupplierService supplierService,
                          PurchaseReceiptService receiptService) {
        this.appUserRepository = appUserRepository;
        this.authService = authService;
        this.branchService = branchService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.productService = productService;
        this.supplierService = supplierService;
        this.receiptService = receiptService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.existsByEmailIgnoreCase(OWNER_A_EMAIL)) {
            log.info("Dữ liệu mẫu đã có, bỏ qua seed");
            return;
        }
        seedTenantA();
        seedTenantB();
        log.info("Đã nạp dữ liệu mẫu: owner.a@oism.vn / owner.b@oism.vn, mật khẩu {}", PASSWORD);
    }

    private void seedTenantA() {
        RegisterResult reg = authService.register(new RegisterCommand(
                "Demo Shop A", "Chu Shop A", OWNER_A_EMAIL, "0900000001", PASSWORD));
        UUID tenantId = reg.tenantId();
        UUID mainBranch = reg.branchId();
        UUID warehouse = branchService.create(tenantId, "Kho tong", BranchType.WAREHOUSE, "Quan 7, TP HCM").id();
        branchService.create(tenantId, "Cua hang Quan 1", BranchType.STORE, "Quan 1, TP HCM");

        userService.create(tenantId, "staff.a@oism.vn", "0900000002", PASSWORD, "Nhan vien kho A", Role.STAFF, warehouse);
        userService.create(tenantId, "cashier.a@oism.vn", "0900000003", PASSWORD, "Thu ngan A", Role.CASHIER, mainBranch);

        List<SupplierView> suppliers = List.of(
                supplierService.create(tenantId, "Cong ty May Viet", "0281111111", "Binh Tan, TP HCM"),
                supplierService.create(tenantId, "Xuong May Hoa Binh", "0282222222", "Tan Phu, TP HCM"),
                supplierService.create(tenantId, "Dai ly Phu Kien Sai Gon", "0283333333", "Quan 5, TP HCM"));

        List<CategoryView> categories = List.of(
                categoryService.create(tenantId, "Thoi trang nam", null),
                categoryService.create(tenantId, "Thoi trang nu", null),
                categoryService.create(tenantId, "Ao", null),
                categoryService.create(tenantId, "Quan", null),
                categoryService.create(tenantId, "Phu kien", null));
        BrandView brand = brandService.create(tenantId, "OISM Basic");

        List<SkuView> allSkus = new ArrayList<>();
        for (int i = 0; i < PRODUCT_NAMES.length; i++) {
            CategoryView category = categories.get(i % categories.size());
            List<SkuInput> inputs = new ArrayList<>();
            for (int j = 0; j < SIZES.length; j++) {
                int serial = i * SIZES.length + j + 1;
                String first12 = "893" + String.format("%09d", serial);
                inputs.add(new SkuInput(PRODUCT_CODES[i] + "-" + SIZES[j], Map.of("size", SIZES[j]),
                        Ean13.complete(first12), BigDecimal.valueOf(150_000L + i * 20_000L + j * 5_000L),
                        BigDecimal.valueOf(120_000L + i * 15_000L), 5));
            }
            ProductView product = productService.create(tenantId, PRODUCT_NAMES[i],
                    "Hang mau cho demo", category.id(), brand.id(), true, inputs);
            allSkus.addAll(product.skus());
        }

        // Phieu nhap 1: toan bo 50 SKU, gia nhap = 60% gia ban; phieu 2: 5 SKU dau voi gia nhap khac de thay gia von binh quan
        List<ReceiptLine> first = allSkus.stream()
                .map(s -> new ReceiptLine(s.id(), 20, s.retailPrice().multiply(BigDecimal.valueOf(0.6))))
                .toList();
        confirm(tenantId, reg.userId(), mainBranch, suppliers.get(0).id(), first);
        List<ReceiptLine> second = allSkus.stream().limit(5)
                .map(s -> new ReceiptLine(s.id(), 10, s.retailPrice().multiply(BigDecimal.valueOf(0.7))))
                .toList();
        confirm(tenantId, reg.userId(), mainBranch, suppliers.get(1).id(), second);
    }

    private void seedTenantB() {
        RegisterResult reg = authService.register(new RegisterCommand(
                "Demo Shop B", "Chu Shop B", "owner.b@oism.vn", "0900000011", PASSWORD));
        UUID tenantId = reg.tenantId();
        userService.create(tenantId, "cashier.b@oism.vn", "0900000012", PASSWORD, "Thu ngan B", Role.CASHIER, reg.branchId());
        CategoryView category = categoryService.create(tenantId, "Do uong", null);
        ProductView product = productService.create(tenantId, "Ca Phe Rang Xay", "Hang mau", category.id(), null, false,
                List.of(new SkuInput("CAPHE-500G", Map.of("khoi_luong", "500g"), null,
                        BigDecimal.valueOf(120_000), null, 3)));
        SupplierView supplier = supplierService.create(tenantId, "Nong trai Da Lat", "0263444444", "Da Lat");
        confirm(tenantId, reg.userId(), reg.branchId(), supplier.id(),
                List.of(new ReceiptLine(product.skus().get(0).id(), 30, BigDecimal.valueOf(80_000))));
    }

    private void confirm(UUID tenantId, UUID userId, UUID branchId, UUID supplierId, List<ReceiptLine> lines) {
        ReceiptView draft = receiptService.create(tenantId, userId, branchId, supplierId, lines);
        receiptService.confirm(tenantId, userId, draft.id());
    }
}
