# RTM v2 — Ma trận truy vết yêu cầu OISM (cuối Sprint 2)

Người phụ trách: M1. Phiên bản v2, lập ngày 04/11/2026 trên nhánh `feature/sprint2`; cập nhật cuối mỗi sprint (RTM v3 ở Sprint 3). Đi kèm: [Kế hoạch kiểm thử v1](K%E1%BA%BF%20ho%E1%BA%A1ch%20ki%E1%BB%83m%20th%E1%BB%AD%20v1.md), [Test case v1](Test%20case%20v1.md), [Báo cáo test thủ công API Sprint 1](B%C3%A1o%20c%C3%A1o%20test%20th%E1%BB%A7%20c%C3%B4ng%20API%20Sprint%201.md).

Nguồn yêu cầu: mục 2–3 của *Kế hoạch dự án OISM – 5 Sprint, 7 thành viên* (44 mã: 33 FR, 11 NFR). Mọi đường dẫn "Mã nguồn" tính từ `backend/src/main/java/com/oism/` (viết tắt `…/`), migration ở `backend/src/main/resources/db/migration/`, test ở `backend/src/test/java/com/oism/`.

## 1. Quy ước

- **Trạng thái:** *Xong* = có mã nguồn và có test tự động chạy xanh trong lần `./mvnw clean verify` ngày 04/11/2026 (62 unit + 25 IT, 0 lỗi); *Một phần* = đã làm một phần phạm vi của yêu cầu (phần còn lại nêu ở ghi chú dưới bảng); *Chưa — Sprint n* = chưa làm, đã xếp vào Sprint n theo mục 6–7 của kế hoạch.
- Cột **Test** ghi "chưa có test" khi chưa có test nào; không ghi test dự kiến.
- Người phụ trách theo mục 7 của kế hoạch: M1 Dung Hoang, M2 Nguyễn Đức Thuận, M3 Trần Ngọc Lâm, M4 Lê Quốc Khánh, M5 Trương Quang Vinh, M6 Phạm Chấn Nguyên, M7 Nghĩa Chu.

## 2. Yêu cầu chức năng

### 2.1 AUTH

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-AUTH-01 | Đăng ký tenant + Owner + chi nhánh mặc định trong một giao dịch | Kế hoạch 2.1; `V1__auth_foundation.sql` (tenant, branch, app_user) | `…/application/auth/AuthService.java`, `…/web/auth/AuthController.java`, `…/domain/model/Tenant.java` | TC-AUTH-01..03: `AuthServiceTest`, `AuthFlowIT`; bằng chứng thủ công: [báo cáo test thủ công](B%C3%A1o%20c%C3%A1o%20test%20th%E1%BB%A7%20c%C3%B4ng%20API%20Sprint%201.md) | M2 | Xong |
| FR-AUTH-02 | Đăng nhập (email/SĐT), refresh xoay vòng, đăng xuất, khóa tài khoản | Kế hoạch 2.1; `V1` (refresh_token) | `…/application/auth/AuthService.java`, `…/application/auth/AccessTokenIssuer.java`, `…/infrastructure/security/JwtService.java`, `…/infrastructure/security/JwtAuthenticationFilter.java`, `…/domain/model/RefreshToken.java` | TC-AUTH-04..10: `AuthServiceTest`, `JwtServiceTest`, `AuthFlowIT`, `RbacIT`; thủ công: [báo cáo test thủ công](B%C3%A1o%20c%C3%A1o%20test%20th%E1%BB%A7%20c%C3%B4ng%20API%20Sprint%201.md) (26 ca, 25 Đạt) | M2 | Xong |
| FR-AUTH-03 | RBAC Owner/Staff/Cashier, sai quyền → 403 `AU40301` | Kế hoạch 2.1 | `…/infrastructure/security/SecurityConfig.java` (`@EnableMethodSecurity`), `@PreAuthorize` trong các controller `…/web/**`, `…/web/error/RestAccessDeniedHandler.java` | TC-AUTH-11, 12: `RbacIT`; thủ công: ca 18..22 của báo cáo test thủ công | M2 | Một phần |
| FR-AUTH-04 | Quản lý chi nhánh/kho: tạo, sửa, ẩn/hiện | Kế hoạch 2.1; `V1` (branch) | `…/application/branch/BranchService.java`, `…/web/branch/BranchController.java` | TC-AUTH-13, 14: `RbacIT` | M3 | Xong |
| FR-AUTH-05 | Quản lý tài khoản nhân viên: tạo, khóa/mở, đặt lại mật khẩu | Kế hoạch 2.1; `V1` (app_user) | `…/application/user/UserService.java`, `…/web/user/UserController.java` | TC-AUTH-15, 16: `RbacIT`; thủ công: ca 22..25 của báo cáo test thủ công | M3 | Một phần |

Ghi chú AUTH:
- FR-AUTH-03 *Một phần*: API đã có test đủ; phần "Admin ẩn menu không được phép" chưa kiểm tra (TC-UI-01 mới xác nhận đăng nhập và các màn hiển thị dữ liệu mẫu, chưa duyệt menu theo từng vai trò; ẩn menu theo vai trò xếp ở Sprint 4).
- FR-AUTH-05 *Một phần*: quy tắc "Cashier chỉ bán tại chi nhánh được gán" mới được ép lúc tạo tài khoản (bắt buộc có chi nhánh hợp lệ, `UserService.assertBranchRule`); kiểm tra lúc bán đi cùng POS checkout (Sprint 4). Tạo, khóa/mở, đặt lại mật khẩu đã có test.
- Quan sát L-01 (báo cáo thủ công): số điện thoại cũng duy nhất toàn hệ thống và trả `AU40901`, trong khi kế hoạch chỉ nêu email duy nhất. Không phải lỗi; cần bổ sung vào kế hoạch và bảng mã lỗi.
- Quan sát L-02 (báo cáo thủ công): access token đã cấp trước khi khóa tài khoản vẫn dùng được đến khi hết hạn (tối đa 60 phút). Là hệ quả của JWT không trạng thái, chưa có yêu cầu nào cấm; ghi nhận để nhóm quyết định.

### 2.2 PROD

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-PROD-01 | Danh mục nhiều cấp, thương hiệu; không xóa khi còn sản phẩm (`PR40902`) | Kế hoạch 2.2; `V2__prod_catalog.sql` (category, brand) | `…/application/catalog/CategoryService.java`, `…/application/catalog/BrandService.java`, `…/web/catalog/CategoryController.java`, `…/web/catalog/BrandController.java` | TC-PROD-13..15: `ProductApiIT.categoryTreeNamesAndDeleteRules` | M3 | Xong |
| FR-PROD-02 | Sản phẩm đơn / có biến thể, mỗi biến thể là SKU; mã SKU duy nhất (`PR40901`); tên khớp regex | Kế hoạch 2.2; `V2` (product, sku) | `…/application/catalog/ProductService.java`, `…/application/catalog/SkuService.java`, `…/web/catalog/ProductController.java`, `…/web/catalog/SkuController.java`, `…/web/catalog/SkuRequest.java` | TC-PROD-01..06: `ProductApiIT`, `TenantIsolationIT` | M3 | Xong |
| FR-PROD-03 | Mã vạch EAN-13 tự sinh có check digit / nhập tay; duy nhất trong tenant; in tem | Kế hoạch 2.2; `V2` (sku.barcode) | `…/domain/service/Ean13.java`, `…/application/catalog/SkuService.java`, `…/web/catalog/SkuController.java` | TC-PROD-07, 08: `Ean13Test`, `ProductApiIT` | M3 (API), M6 (in tem Admin) | Một phần |
| FR-PROD-04 | Giá lẻ/giá sỉ > 0; đổi giá không đổi đơn đã tạo | Kế hoạch 2.2; `V2` (sku), `V4` (order_item.unit_price) | `…/application/catalog/SkuService.java`, `…/domain/model/OrderItem.java` | TC-PROD-09, 10: `ProductApiIT.changesPricesWithoutAffectingExistingOrders` | M3 | Xong |
| FR-PROD-05 | Tìm theo tên/SKU/mã vạch, lọc, phân trang 20 dòng | Kế hoạch 2.2 | `…/application/catalog/ProductService.java`, `…/application/catalog/SkuService.java`, `…/application/common/PageResult.java`, `…/application/common/Paging.java` | TC-PROD-11, 12: `ProductApiIT.searchesByNameSkuAndBarcodeWithPaging` (cả lọc theo trạng thái) | M3 | Một phần |

Ghi chú PROD: FR-PROD-03 *Một phần* vì "in tem mã vạch từ Admin" (TC-UI-02) chưa chạy tay; phần API đã có test. FR-PROD-05 *Một phần* vì bộ lọc theo danh mục và thương hiệu (`ProductController`, tham số `categoryId`, `brandId`) chưa có test; `ProductApiIT` mới kiểm tra tìm kiếm, phân trang và lọc theo trạng thái.

### 2.3 INV

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-INV-01 | Sổ cái tồn kho chỉ ghi thêm, có `balance_after`, khóa dòng tồn | Kế hoạch 2.3, mục 4; `V3__inv_ledger.sql` (stock_balance, inventory_transaction, trigger `trg_invtx_append_only`) | `…/application/inventory/InventoryLedgerService.java` (`post`), `…/application/inventory/LedgerEntry.java`, `…/domain/model/InventoryTransaction.java`, `…/domain/model/StockBalance.java`, `…/web/inventory/StockController.java` | TC-INV-03, 08: `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel`, `InventoryIT.concurrentOutNeverOversells` | M4 | Xong |
| FR-INV-02 | Phiếu nhập Nháp → Đã xác nhận; chỉ khi xác nhận mới ghi sổ cái; phiếu đã xác nhận không sửa (`IV40902`) | Kế hoạch 2.3; `V3` (purchase_receipt, purchase_receipt_item) | `…/application/inventory/PurchaseReceiptService.java` (`create`, `update`, `confirm`), `…/web/inventory/PurchaseReceiptController.java`, `…/domain/model/PurchaseReceipt.java` | TC-INV-01, 04..06: `InventoryIT` | M4 | Xong |
| FR-INV-03 | Chuyển kho 2 bước | Kế hoạch 2.3; `V3` (stock_transfer, stock_transfer_item) — mới có bảng | Chưa có lớp nào (chỉ có bảng) | Chưa có test | M4 | Chưa — Sprint 3 |
| FR-INV-04 | Kiểm kê: tạo phiên, duyệt, bút toán Điều chỉnh | Kế hoạch 2.3; `V3` (stocktake, stocktake_item) — mới có bảng | Chưa có lớp nào (chỉ có bảng); lý do `ADJUST` đã có trong ràng buộc sổ cái | Chưa có test | M4 | Chưa — Sprint 3 |
| FR-INV-05 | Nhà cung cấp: thêm, sửa, ngưng dùng | Kế hoạch 2.3; `V3` (supplier) | `…/application/inventory/SupplierService.java`, `…/web/inventory/SupplierController.java`, `…/domain/model/Supplier.java` | TC-INV-07: `InventoryIT.validatesReceiptInputAndSuppliers` | M4 | Xong |

### 2.4 COST

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-COST-01 | Giá vốn bình quân gia quyền khi xác nhận phiếu nhập, làm tròn 2 chữ số | Kế hoạch 2.4, mục 4 | `…/domain/service/CostCalculator.java` (`weightedAverage`), `…/application/inventory/PurchaseReceiptService.java` (`confirm`) | TC-INV-01, 02, 09: `CostCalculatorTest`, `InventoryIT.sprint2Acceptance_...`, `InventoryIT.concurrentReceiptsOnSameSkuKeepAverageCostConsistent` | M4 | Xong |
| FR-COST-02 | Chụp giá vốn vào `order_item.cost_price` khi đơn Confirmed / thanh toán POS | Kế hoạch 2.4; `V4` (cột `order_item.cost_price` đã có) | Chưa có (đơn mới ở trạng thái Draft) | Chưa có test | M4 + M5 | Chưa — Sprint 3 |

### 2.5 ORD (phần Draft + state machine)

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-ORD-01 | Tiếp nhận đơn đa kênh về một mô hình chung; chống trùng mã đơn ngoài | Kế hoạch 2.5; `V4__ord_orders.sql` (orders, order_item, order_status_history, payment) | `…/application/order/OrderService.java` (`createDraft`), `…/web/order/OrderController.java`, `…/domain/model/SalesOrder.java`, `…/domain/model/OrderChannel.java` | TC-ORD-01, 02: `OrderApiIT` | M5 | Một phần |
| FR-ORD-02 | Chuyển trạng thái đúng bảng; sai bước → `OD40901`; ghi lịch sử | Kế hoạch 2.5, mục 4; `V4` | `…/domain/service/OrderStateMachine.java`, `…/domain/model/OrderStatus.java`, `…/domain/model/OrderStatusHistory.java` | TC-ORD-03: `OrderStateMachineTest`; lịch sử bước đầu: `OrderApiIT.createsDraftOrderWithTotalAndHistory` | M5 | Một phần |
| FR-ORD-03 | Xử lý đơn: danh sách lọc, chi tiết, duyệt/hoàn tất/hủy, in phiếu giao | Kế hoạch 2.5 | `…/application/order/OrderService.java` (`get`, `list`), `…/web/order/OrderController.java` | TC-ORD-04: `OrderApiIT.listsOrdersWithFiltersAndPaging` | M5 (API), M6 (Admin) | Một phần |

Ghi chú ORD:
- Chỉ có tạo đơn Draft, xem chi tiết, danh sách có lọc và `OrderStateMachine`; chưa có API giữ/duyệt/hoàn tất/hủy, chưa nối state machine vào dịch vụ.
- FR-ORD-01 còn thiếu webhook Shopee/TikTok Shop và chống trùng mã đơn ngoài (Sprint 3); FR-ORD-02 còn thiếu API chuyển trạng thái (Sprint 3); FR-ORD-03 còn thiếu các nút xử lý và in phiếu giao (Sprint 3).

### 2.6 RSE

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-RSE-01 | `available = on_hand − reserved`, không âm | Kế hoạch 2.6; `V3` (cột `reserved`, ràng buộc `ck_stock_balance`) | `…/domain/model/StockBalance.java`, `…/application/inventory/StockService.java` (tra `available`) | Ràng buộc DB được kiểm gián tiếp qua `InventoryIT.concurrentOutNeverOversells`; chưa có test riêng cho `reserved` | M5 | Một phần |
| FR-RSE-02 | Giữ hàng, khóa `FOR UPDATE`, hạn giữ, thiếu → `IV40901` | Kế hoạch 2.6 | Chưa có `ReservationService` | Chưa có test | M5 | Chưa — Sprint 3 |
| FR-RSE-03 | Trừ và nhả hàng khi Confirmed/Cancelled trong một giao dịch | Kế hoạch 2.6 | Chưa có | Chưa có test | M5 | Chưa — Sprint 3 |

Ghi chú RSE: FR-RSE-01 *Một phần* vì mới có cột `reserved` và ràng buộc CHECK ở CSDL; chưa có luồng nào thay đổi `reserved`. Mã lỗi `IV40901` hiện chỉ dùng cho xuất kho vượt tồn ở sổ cái (xem TC-INV-08).

### 2.7 POS

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-POS-01 | Giao diện bán nhanh: tìm, quét mã + Enter, F2/F9/Esc, PWA | Kế hoạch 2.7 | `frontend-pos/src/SaleScreen.tsx`, `frontend-pos/src/cart.ts`, `frontend-pos/src/api.ts`, `frontend-pos/src/LoginScreen.tsx` | `frontend-pos/src/cart.test.ts` (6 test Vitest cho giỏ hàng); thao tác bàn phím: TC-UI-04 chưa chạy tay | M7 | Một phần |
| FR-POS-02 | Chặn bán vượt `available` | Kế hoạch 2.7 | Tra `available` qua `…/web/catalog/SkuController.java` (lookup) và `…/application/catalog/SkuLookupView.java`; chặn ở giao diện chưa kiểm chứng | TC-PROD-08 (lookup trả `available`); chưa có test cho phần chặn khi bán | M7 | Một phần |
| FR-POS-03 | Thanh toán tiền mặt/QR, in hóa đơn 80 mm | Kế hoạch 2.7; `V4` (payment) — mới có bảng | Chưa có | Chưa có test | M7 | Chưa — Sprint 4 |
| FR-POS-04 | Thanh toán một giao dịch (tạo → giữ → xác nhận → trừ kho → ghi thanh toán) | Kế hoạch 2.7 | Chưa có `POST /pos/checkout` | Chưa có test | M5 | Chưa — Sprint 4 |

Ghi chú POS: giao diện POS Sprint 2 mới có tìm kiếm và giỏ hàng; chưa chạy tay trên trình duyệt (TC-UI-04). Service worker và cài PWA xếp ở Sprint 3–4 theo mục 7.

### 2.8 REP và 2.9 SIM

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| FR-REP-01 | Doanh thu và lợi nhuận gộp, xuất CSV | Kế hoạch 2.8 | Chưa có | Chưa có test | M3 | Chưa — Sprint 3 |
| FR-REP-02 | Giá trị tồn, top bán chạy, hàng tồn chậm | Kế hoạch 2.8 | Chưa có (đã có API xem tồn `StockController`, chưa phải báo cáo) | Chưa có test | M3 | Chưa — Sprint 4 |
| FR-REP-03 | Cảnh báo tồn thấp | Kế hoạch 2.8 | Chưa có | Chưa có test | M4 | Chưa — Sprint 4 |
| FR-SIM-01 | Webhook Simulator, xác thực API key tenant | Kế hoạch 2.9 | Chưa có | Chưa có test | M5 (API), M7 (màn Admin) | Chưa — Sprint 3 |
| FR-SIM-02 | Thông báo thời gian thực qua WebSocket | Kế hoạch 2.9 | Chưa có | Chưa có test | M2 | Chưa — Sprint 4 |
| FR-SIM-03 | Tác vụ nền: hủy đơn giữ quá hạn, báo cáo ngày | Kế hoạch 2.9 | Chưa có | Chưa có test | M5 | Chưa — Sprint 4 |

## 3. Yêu cầu phi chức năng

| Mã | Yêu cầu (rút gọn) | Thiết kế | Mã nguồn (lớp chính) | Test | Người phụ trách | Trạng thái |
|---|---|---|---|---|---|---|
| NFR-PERF-01 | p95 tìm/quét < 200 ms, ghi < 500 ms, báo cáo < 2 s | Kế hoạch 3 | Chưa có | Chưa có test (chưa có JMeter) | M4, M7 | Chưa — Sprint 5 |
| NFR-PERF-02 | 50 đơn đồng thời vào 1 SKU còn 1 cái: 1 thành công, 49 `IV40901` | Kế hoạch 3; `V3` (khóa dòng tồn) | Phần sổ cái: `…/application/inventory/InventoryLedgerService.java` (`post`) | `InventoryIT.concurrentOutNeverOversells` (50 luồng xuất kho, mức sổ cái); phiên bản mức đơn hàng chưa có | M5 | Một phần |
| NFR-TENANT-01 | Cô lập dữ liệu theo tenant, truy cập chéo → 404 | Kế hoạch 3, mục 4 | `…/infrastructure/tenant/TenantContext.java`, `…/infrastructure/tenant/TenantFilterAspect.java`, `…/domain/model/TenantScopedEntity.java` | TC-TEN-01: `TenantIsolationIT` | M2 | Xong |
| NFR-TENANT-02 | Index `(tenant_id, created_at)`, `(tenant_id, sku_id)`; sổ cái < 500 ms với 1 triệu dòng | Kế hoạch 3; `V1`..`V4` (các index `ix_*_tenant_created`, `ix_invtx_tenant_sku`) | Index nằm trong migration | Chưa có test (chưa `EXPLAIN ANALYZE`, chưa có dữ liệu 1 triệu dòng) | M2, M4 | Một phần |
| NFR-SEC-01 | BCrypt, HTTPS, access token 60 phút, refresh 7 ngày xoay vòng, lưu băm | Kế hoạch 3 | `…/infrastructure/security/SecurityConfig.java` (BCrypt), `…/application/auth/AuthService.java` (băm refresh token, xoay vòng), `…/infrastructure/security/JwtService.java` | TC-AUTH-04, 07, 08: `AuthServiceTest`, `JwtServiceTest`, `AuthFlowIT`; HTTPS chưa cấu hình | M2 | Một phần |
| NFR-SEC-02 | Toàn vẹn giao dịch cho chuỗi Tạo đơn → Giữ → Trừ | Kế hoạch 3 | Chưa có (chuỗi chưa tồn tại); `InventoryLedgerService.post` và `PurchaseReceiptService.confirm` đã chạy trong một giao dịch | Chưa có test giả lập lỗi | M2 | Chưa — Sprint 3 |
| NFR-SEC-03 | Sổ cái chỉ ghi thêm: không API sửa/xóa, trigger chặn `UPDATE`/`DELETE` | Kế hoạch 3; `V3` (trigger `trg_invtx_append_only`) | `…/web/inventory/StockController.java` (chỉ có GET), `…/domain/repository/InventoryTransactionRepository.java` | TC-INV-03: `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel` | M4 | Xong |
| NFR-USA-01 | Sẵn sàng 99,5% giờ 7:00–22:00 | Kế hoạch 3 | `docker-compose.yml` có `healthcheck` và `restart: unless-stopped`; chưa kiểm thử vận hành | Chưa có test | M2 | Chưa — Sprint 5 |
| NFR-USA-02 | POS thanh toán ≤ 3 thao tác; chạy trên máy tính, tablet, điện thoại | Kế hoạch 3 | Chưa có (chưa có màn thanh toán) | Chưa có test (chưa có Playwright) | M7 | Chưa — Sprint 5 |
| NFR-MAINT-01 | Clean Architecture; `domain` không phụ thuộc `web`/`infrastructure`; controller không gọi repository | Kế hoạch 3, mục 1 | Cấu trúc 4 gói `…/domain`, `…/application`, `…/infrastructure`, `…/web` | TC-ARC-01: `ArchitectureTest` (6 luật) | M2 | Xong |
| NFR-MAINT-02 | Phủ test ≥ 80% sổ cái, giữ hàng, giá vốn; pipeline đỏ nếu dưới ngưỡng | Kế hoạch 3 | `backend/pom.xml` (JaCoCo `prepare-agent` + `report`), `.github/workflows/ci.yml` | Chưa có ngưỡng chặn (chỉ sinh báo cáo); workflow CI chưa chạy trên GitHub | M2, M4 | Một phần |

Ghi chú: NFR-MAINT-02 *Một phần*, phần chặn merge theo ngưỡng ở Sprint 5. Workflow `.github/workflows/ci.yml` đã viết nhưng chưa chạy trên GitHub, nên "CI chạy xanh" của nghiệm thu Sprint 2 mới được xác nhận bằng lần chạy cục bộ.

## 4. Thống kê

| Trạng thái | Số mã |
|---|---|
| Xong | 13 |
| Một phần | 14 |
| Chưa — Sprint 3 | 8 |
| Chưa — Sprint 4 | 6 |
| Chưa — Sprint 5 | 3 |
| Tổng | 44 |

Chưa có collection Postman; kiểm thử API hiện dựa trên test tích hợp tự động và [báo cáo test thủ công Sprint 1](B%C3%A1o%20c%C3%A1o%20test%20th%E1%BB%A7%20c%C3%B4ng%20API%20Sprint%201.md). Các case giao diện TC-UI-02..04 chưa chạy tay (xem cuối Test case v1).
