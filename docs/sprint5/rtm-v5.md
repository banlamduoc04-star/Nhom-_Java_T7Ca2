# MA TRẬN TRUY XUẤT YÊU CẦU (RTM v5 – SPRINT 5)
**Dự án:** OISM (Omnichannel Inventory and Sales Management System)  
**Tác giả:** M1 – Dung Hoang  
**Ngày phát hành:** 11/11/2026  
**Thay cho:** `docs/sprint4/rtm-v4.md`

Nguồn yêu cầu: `docs/ke-hoach/ke-hoach-du-an-oism.md` (mục 2, 3). Trạng thái lấy theo kết quả chạy thật ngày 11/11/2026 trên nhánh Sprint 5:
- `./mvnw verify`: 115 unit + 59 tích hợp (PostgreSQL thật qua Testcontainers) đạt; cổng coverage ≥ 80% đạt.
- Postman/newman: 149 request, 244 assertion đạt. E2E Playwright: 7/7. Hiệu năng và UAT trên bản Docker.
- Chi tiết: `docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md`, `docs/sprint4/kich-ban-bao-cao-uat-sprint4.md`, `docs/sprint3/kich-ban-flash-sale.md`.

**Quy ước trạng thái:** *Đạt* = có test tự động hoặc lần chạy ghi nhận chứng minh đủ tiêu chí nghiệm thu; *Đạt một phần* = phần chính đạt, phần còn lại chưa làm hoặc chưa có bằng chứng (ghi rõ ở cột Ghi chú).

## 1. Yêu cầu chức năng

| Mã | Tên | Lớp backend chính | Giao diện | Bằng chứng kiểm thử | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| FR-AUTH-01 | Đăng ký cửa hàng (tenant + Owner + chi nhánh mặc định) | `AuthService.register` | `RegisterPage` | `AuthServiceTest.registerCreatesTenantBranchAndOwner`, `AuthFlowIT`, Postman 0 | Đạt | |
| FR-AUTH-02 | Đăng nhập email/SĐT, làm mới, đăng xuất | `AuthService` | `LoginPage`, `client.ts` | `AuthServiceTest` (SĐT, xoay vòng, dùng lại token cũ, khóa AU40302), `AuthFlowIT.registerLoginMeRefreshAndLogout`, Postman 0 | Đạt | |
| FR-AUTH-03 | Phân quyền RBAC | `@PreAuthorize`, `SecurityConfig` | `AppLayout`, `guards.tsx` | `RbacIT`, `PosCheckoutIT`, `ReportApiIT`, Postman (mọi case 403 `AU40301`) | Đạt | |
| FR-AUTH-04 | Quản lý chi nhánh/kho | `BranchService` | `BranchesPage` | `RbacIT.ownerManagesBranchesAndEmployees`, `cannotHideBranchWithStockOrOpenOrders` (AU40902) | Đạt | |
| FR-AUTH-05 | Tài khoản nhân viên; Cashier chỉ bán tại chi nhánh gán | `UserService`, `PosCheckoutService.assertCanSellAt` | `UsersPage` | `RbacIT`, `PosCheckoutIT`, `PosCheckoutServiceTest.cashierCannotSellAtOtherBranch`, Postman 1 | Đạt | Ràng buộc chi nhánh của Cashier thêm ở Sprint 5 |
| FR-PROD-01 | Danh mục nhiều cấp, thương hiệu | `CategoryService`, `BrandService` | `CategoriesPage`, `BrandsPage` | `ProductApiIT.categoryTreeNamesAndDeleteRules` (PR40902), Postman M3 1–2 | Đạt | |
| FR-PROD-02 | Sản phẩm đơn / biến thể (SKU) | `ProductService`, `SkuService` | `ProductsPage`, `ProductDetailPage` | `ProductApiIT.createsProductWithThreeVariantsInOneCall`, `rejectsDuplicateSkuCodeAndBarcode`, E2E Admin (tạo sản phẩm) | Đạt | |
| FR-PROD-03 | Mã vạch EAN-13 / nhập tay, in tem | `Ean13`, `SkuService` | `BarcodeLabel` | `Ean13Test`, `ProductApiIT.generatesValidEan13AndLooksUpByBarcode` | Đạt một phần | In tem trên Admin chưa có test tự động |
| FR-PROD-04 | Giá lẻ, giá sỉ; đổi giá không ảnh hưởng đơn cũ | `Sku`, `SkuService` | `ProductDetailPage` | `ProductApiIT.changesPricesWithoutAffectingExistingOrders` | Đạt | |
| FR-PROD-05 | Tìm kiếm, lọc, phân trang | `ProductService`, `SkuService` | `ProductsPage` | `ProductApiIT.searchesByNameSkuAndBarcodeWithPaging`; p95 tìm SKU 37,6 ms | Đạt | |
| FR-INV-01 | Sổ cái chỉ ghi thêm | `InventoryLedgerService.post` | `StockPage` | `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel`, `LedgerKeysetIT`; coverage 95,8% | Đạt | Keyset trước Sprint 5 lỗi 500, đã sửa |
| FR-INV-02 | Phiếu nhập Nháp → Đã xác nhận | `PurchaseReceiptService` | `ReceiptsPage` | `InventoryIT` (IV40902), E2E Admin (nhập kho), Postman 3 | Đạt | |
| FR-INV-03 | Chuyển kho 2 bước | `StockTransferService` | `TransfersPage` | `StockTransferServiceTest`, `OrderLifecycleIT.draftTransferCanBeCancelledButDispatchedCannot`, Postman 4 | Đạt | |
| FR-INV-04 | Kiểm kê, Owner duyệt | `StocktakeService` | `StocktakesPage` | `StocktakeServiceTest`, Postman 4 (Staff duyệt 403, sửa sau duyệt 409) | Đạt | Màn Admin gửi sai tên trường số đếm, đã sửa ở Sprint 5 |
| FR-INV-05 | Nhà cung cấp | `SupplierService` | `SuppliersPage` | `InventoryIT.validatesReceiptInputAndSuppliers`, Postman 3 | Đạt | |
| FR-COST-01 | Giá vốn bình quân gia quyền | `CostCalculator`, `PurchaseReceiptService.confirm` | `ReceiptsPage` | `CostCalculatorTest` (13 ví dụ), `InventoryIT.sprint2Acceptance_…`, `concurrentReceiptsOnSameSkuKeepAverageCostConsistent`; coverage 100% | Đạt | |
| FR-COST-02 | Chụp giá vốn vào dòng đơn | `ReservationService.confirm` | `OrdersPage` | `ReservationServiceTest.confirm_…capturesAvgCost…`, UAT kịch bản 1 | Đạt | |
| FR-ORD-01 | Tiếp nhận đơn đa kênh, chống trùng | `OrderService`, `WebhookController` | `OrdersPage`, `SimulatorPage` | `OrderApiIT`, `WebhookIT` (OD40902), Postman 5, 7 | Đạt | Kênh: POS/MANUAL/SHOPEE/TIKTOK (đã bỏ LAZADA ngoài kế hoạch) |
| FR-ORD-02 | Máy trạng thái đơn | `OrderStateMachine` | `OrdersPage` | `OrderStateMachineTest` (20 case), Postman 5 (OD40901) | Đạt | |
| FR-ORD-03 | Xử lý đơn, lọc, lịch sử, phiếu giao | `OrderService`, `ShippingSlipService` | `OrdersPage` | `OrderApiIT.listsOrdersWithFiltersAndPaging`, `OrderDocumentsIT`, E2E Admin (duyệt đơn) | Đạt | API phiếu giao thêm ở Sprint 5 |
| FR-RSE-01 | Hàng khả dụng = on_hand − reserved | `StockBalance` | `StockPage`, `SaleScreen` | `ReservationServiceTest`, `FlashSaleIT` | Đạt | |
| FR-RSE-02 | Giữ hàng có khóa, thứ tự SKU | `ReservationService.reserve` | — | `FlashSaleIT`, `OrderLifecycleIT.concurrentOrdersLockingSkusInOppositeOrderNeverDeadlock` | Đạt | |
| FR-RSE-03 | Trừ / nhả / trả hàng | `ReservationService` | `OrdersPage` | `OrderLifecycleIT.cancelReservedReleasesAndCancelConfirmedReturnsStockWithLedger`, `TransactionRollbackIT` | Đạt | |
| FR-POS-01 | Bán nhanh: quét + Enter, F2/F9/Esc, PWA | `SkuService.lookup` | `SaleScreen` | E2E POS (quét 2 lần tăng số lượng, F9) ở 3 kích thước; p95 quét 32,6 ms | Đạt một phần | F2, Esc và cài đặt PWA chưa có test tự động |
| FR-POS-02 | Chặn vượt tồn khả dụng | `cart.ts` | `SaleScreen` | `cart.test.ts` (Vitest), E2E POS "thêm vượt tồn…" | Đạt | |
| FR-POS-03 | Tiền mặt/QR, in hóa đơn 80 mm tự động | `PosCheckoutService`, `GET /pos/orders/{id}/receipt` | `SaleScreen`, `QrCode` | E2E POS (tự gọi in), `OrderDocumentsIT.posReceiptReprintRespectsRoleBranchAndTenant`, UAT kịch bản 1 | Đạt | QR sinh tại chỗ; chưa nối ngân hàng thật |
| FR-POS-04 | Thanh toán một giao dịch | `PosCheckoutService.checkout` | `SaleScreen` | `TransactionRollbackIT` (lỗi ở bước Giữ, Trừ, thanh toán), `PosCheckoutIT` | Đạt | |
| FR-REP-01 | Doanh thu, lợi nhuận gộp, CSV | `ProfitReportService`, `SalesAnalyticsRepository` | `ProfitReportPage` | `ProfitReportIT` (số tính tay, lọc, CSV, 70.000 đơn), `ProfitReportServiceTest` | Đạt | Trước Sprint 5: lỗi > 65.535 đơn và lệch dạng dữ liệu với Admin, đã sửa |
| FR-REP-02 | Giá trị tồn, top bán chạy, tồn chậm | `StockReportService` | `StockReportsPage` | `ReportApiIT`, `StockReportServiceTest`, UAT kịch bản 4 | Đạt | Quyền Owner + Staff theo kế hoạch |
| FR-REP-03 | Cảnh báo tồn thấp tự mở/đóng | `StockAlertService` | `DashboardPage`, `AppLayout` | `StockAlertIT`, `StockAlertServiceTest`, UAT kịch bản 2 | Đạt | |
| FR-SIM-01 | Webhook theo API key tenant, Simulator | `WebhookApiKeyFilter`, `ChannelCredentialService`, `SimulatorController` | `SimulatorPage`, `SettingsPage` | `WebhookIT`, `FlashSaleIT.simulator_…`, Postman 7 | Đạt một phần | Màn Simulator trên Admin chưa có E2E |
| FR-SIM-02 | Realtime WebSocket theo tenant | `WebSocketConfig`, `TenantChannelInterceptor`, `RealtimeEventListener` | `AppLayout`, `realtime.ts` (POS) | `RealtimeIT`, `TenantChannelInterceptorTest`, E2E Admin (popup đơn sàn) | Đạt | |
| FR-SIM-03 | Tác vụ nền: hủy đơn quá hạn, chốt sổ ngày | `ReservationExpirationJob`, `DailyReportJob`, `DailyReportService` | `StockReportsPage` | `OrderLifecycleIT.expiredReservation…`, `ReportApiIT`, UAT kịch bản 3 (tự hủy sau 105 giây) | Đạt | |

## 2. Yêu cầu phi chức năng

| Mã | Chỉ số theo kế hoạch | Kết quả đo / bằng chứng | Trạng thái | Ghi chú |
|---|---|---|---|---|
| NFR-PERF-01 | Tìm/quét p95 < 200 ms; ghi < 500 ms; báo cáo < 2 s với 100.000 bản ghi | Quét 32,6 ms; tìm 37,6 ms; tạo đơn 33,9 ms; lợi nhuận 352 ms với 100.000 đơn | Đạt | Đo bằng script trên bản Docker, chưa dùng JMeter/tải song song |
| NFR-PERF-02 | 50 đơn / 1 cái → 1 thành công, 49 `IV40901`, available 0 | `FlashSaleIT` (2 case), Docker: đơn `DH-20261110-8239` | Đạt | |
| NFR-TENANT-01 | Đọc chéo tenant → 404 | `TenantIsolationIT`, case tenant khác trong `WebhookIT`, `OrderDocumentsIT`, `StockAlertIT`, `ReportApiIT`; Postman (404) | Đạt | |
| NFR-TENANT-02 | Index; sổ cái 1 triệu dòng < 500 ms | `EXPLAIN ANALYZE`: dùng index, ≤ 0,24 ms (đếm tổng 41 ms); API sổ cái p95 150 ms | Đạt | V12 thêm index keyset |
| NFR-SEC-01 | BCrypt, HTTPS, token 60 phút, refresh 7 ngày xoay vòng, lưu băm | `AuthServiceTest`, `JwtServiceTest`; API key webhook cũng chỉ lưu băm | Đạt một phần | Chưa cấu hình HTTPS (bản Docker chạy HTTP) |
| NFR-SEC-02 | Lỗi giữa chuỗi Tạo đơn → Giữ → Trừ không để lại dữ liệu dở | `TransactionRollbackIT` (4 case, lỗi CSDL thật) | Đạt | |
| NFR-SEC-03 | Sổ cái không sửa/xóa | `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel` (trigger) | Đạt | |
| NFR-USA-01 | Sẵn sàng 99,5%, healthcheck + tự khởi động lại | `HealthIT`; 4 container `healthy`, `restart: unless-stopped` | Đạt một phần | Chưa đo tỷ lệ sẵn sàng thực tế |
| NFR-USA-02 | POS tối đa 3 thao tác; chạy trên máy tính, tablet, điện thoại | E2E POS: F9 → 500k → Xác nhận ở 1366×768, 768×1024, Pixel 7; không tràn ngang | Đạt | |
| NFR-MAINT-01 | Clean Architecture | `ArchitectureTest` (6 luật) trong CI | Đạt | |
| NFR-MAINT-02 | Coverage ≥ 80% sổ cái, giữ hàng, giá vốn; CI đỏ khi dưới ngưỡng | 95,8% / 84,1% / 100%; JaCoCo `check` trong profile `coverage` mà CI dùng | Đạt | |

## 3. Việc còn mở

- HTTPS khi triển khai (NFR-SEC-01); đo tỷ lệ sẵn sàng (NFR-USA-01); đo tải bằng JMeter (NFR-PERF-01).
- Test tự động cho F2/Esc, cài đặt PWA, in tem mã vạch, màn Simulator.
- Đưa Postman và E2E vào CI (cần dựng Docker trong pipeline).
