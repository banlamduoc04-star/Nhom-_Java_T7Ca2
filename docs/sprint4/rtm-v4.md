# MA TRẬN TRUY XUẤT YÊU CẦU (RTM v4 - SPRINT 4)
**Dự án:** OISM (Omnichannel Inventory and Sales Management System)  
**Tác giả:** M1 – Dung Hoang  
**Ngày phát hành:** 04/11/2026  
**Trạng thái:** Đã được thay bằng [RTM v5](../sprint5/rtm-v5.md) (11/11/2026). Bản v4 ghi "Đạt" cho FR-SIM-02, FR-REP-03, FR-AUTH-05, FR-SIM-01 nhưng rà soát Sprint 5 cho thấy các mục này chưa đạt lúc đó; giữ nguyên bên dưới làm lịch sử.  

---

## Bảng theo dõi yêu cầu chức năng (Functional Requirements Traceability)

| Mã yêu cầu | Tên chức năng | Sprint triển khai | Phụ trách chính | Trạng thái | Lớp Backend tương ứng | Lớp Frontend tương ứng | Kết quả Test |
|---|---|---|---|---|---|---|---|
| **FR-AUTH-01** | Đăng ký cửa hàng & tenant | Sprint 1 | M2, M3 | Hoàn thành | `AuthService.register()` | `RegisterPage.tsx` | Đạt ✅ |
| **FR-AUTH-02** | Đăng nhập JWT & Refresh xoay vòng | Sprint 1, 4 | M2 | Hoàn thành | `AuthService.login()`, `refresh()` | `LoginPage.tsx`, `client.ts` | Đạt ✅ |
| **FR-AUTH-03** | Phân quyền RBAC (Owner/Staff/Cashier) | Sprint 1 | M2, M6 | Hoàn thành | `@PreAuthorize`, `SecurityConfig` | `AppLayout.tsx`, `guards.tsx` | Đạt ✅ |
| **FR-AUTH-04** | Quản lý Chi nhánh/Kho | Sprint 1, 4 | M3, M6 | Hoàn thành | `BranchController.java` | `BranchesPage.tsx` | Đạt ✅ |
| **FR-AUTH-05** | Quản lý Nhân viên & gán chi nhánh | Sprint 1, 4 | M3, M6 | Hoàn thành | `UserController.java` | `UsersPage.tsx` | Đạt ✅ |
| **FR-PROD-01** | Danh mục & Thương hiệu | Sprint 2 | M3, M6 | Hoàn thành | `CategoryController`, `BrandController` | `CategoriesPage`, `BrandsPage` | Đạt ✅ |
| **FR-PROD-02** | Sản phẩm đơn & Biến thể (SKU) | Sprint 2 | M3, M6 | Hoàn thành | `ProductService`, `SkuService` | `ProductsPage`, `ProductDetailPage` | Đạt ✅ |
| **FR-PROD-03** | Mã vạch chuẩn EAN-13 | Sprint 2 | M3 | Hoàn thành | `Ean13.java`, `SkuController` | `ProductDetailPage.tsx` | Đạt ✅ |
| **FR-PROD-04** | Bảng giá bán lẻ & giá sỉ | Sprint 2 | M3 | Hoàn thành | `Sku.retailPrice`, `wholesalePrice` | `ProductDetailPage.tsx` | Đạt ✅ |
| **FR-INV-01** | Sổ cái tồn kho chỉ ghi thêm (Append-only) | Sprint 2, 4 | M4 | Hoàn thành | `InventoryLedgerService.post()` | `StockPage.tsx` | Đạt ✅ |
| **FR-INV-02** | Phiếu nhập kho từ Nhà cung cấp | Sprint 2 | M4, M6 | Hoàn thành | `PurchaseReceiptService` | `ReceiptsPage.tsx` | Đạt ✅ |
| **FR-INV-03** | Chuyển kho liên chi nhánh 2 bước | Sprint 3 | M4, M6 | Hoàn thành | `StockTransferService` | `TransfersPage.tsx` | Đạt ✅ |
| **FR-INV-04** | Kiểm kê kho & Bút toán điều chỉnh | Sprint 3 | M4, M6 | Hoàn thành | `StocktakeService` | `StocktakesPage.tsx` | Đạt ✅ |
| **FR-COST-01** | Giá vốn bình quân gia quyền | Sprint 2 | M4 | Hoàn thành | `CostCalculator.java` | `ReceiptsPage.tsx` | Đạt ✅ |
| **FR-COST-02** | Chụp giá vốn bất biến vào dòng đơn | Sprint 3 | M4, M5 | Hoàn thành | `ReservationService.confirm()` | `OrdersPage.tsx` | Đạt ✅ |
| **FR-ORD-01** | Tiếp nhận đơn hàng đa kênh | Sprint 3 | M5 | Hoàn thành | `OrderService`, `WebhookController` | `OrdersPage`, `SimulatorPage` | Đạt ✅ |
| **FR-ORD-02** | Máy trạng thái đơn hàng (State Machine) | Sprint 3 | M5 | Hoàn thành | `OrderStateMachine.java` | `OrdersPage.tsx` | Đạt ✅ |
| **FR-ORD-03** | Xử lý duyệt, hoàn tất và hủy đơn | Sprint 3 | M5, M6 | Hoàn thành | `OrderService` | `OrdersPage.tsx` | Đạt ✅ |
| **FR-RSE-01** | Hàng khả dụng (Available = OnHand - Reserved) | Sprint 3 | M5 | Hoàn thành | `StockBalance.getAvailable()` | `StockPage.tsx`, `SaleScreen.tsx` | Đạt ✅ |
| **FR-RSE-02** | Khóa bi quan chống bán vượt (Pessimistic Lock) | Sprint 3 | M5 | Hoàn thành | `StockBalanceRepository.findForUpdate` | `ReservationService.java` | Đạt ✅ |
| **FR-RSE-03** | Trừ kho và nhả hàng giữ tự động | Sprint 3, 4 | M5 | Hoàn thành | `ReservationExpirationJob.java` | `OrderService.java` | Đạt ✅ |
| **FR-POS-01** | Giao diện POS bán nhanh (F2, F9, Esc) | Sprint 2, 4 | M7 | Hoàn thành | `SkuController.lookup` | `SaleScreen.tsx` | Đạt ✅ |
| **FR-POS-02** | Ràng buộc tồn khả dụng khi bán POS | Sprint 2, 4 | M7 | Hoàn thành | `cart.ts` | `SaleScreen.tsx` | Đạt ✅ |
| **FR-POS-03** | Thanh toán tiền mặt/QR và in hóa đơn 80mm | Sprint 4 | M7 | Hoàn thành | `Payment.java`, `PosController` | `SaleScreen.tsx` (In bill) | Đạt ✅ |
| **FR-POS-04** | Thanh toán POS trong một giao dịch cơ sở dữ liệu | Sprint 4 | M5, M7 | Hoàn thành | `PosCheckoutService.checkout()` | `SaleScreen.tsx` | Đạt ✅ |
| **FR-REP-01** | Báo cáo doanh thu và lợi nhuận gộp | Sprint 3 | M3, M6 | Hoàn thành | `ProfitReportService.java` | `ProfitReportPage.tsx` | Đạt ✅ |
| **FR-REP-02** | Báo cáo định giá tồn, top bán chạy, tồn chậm | Sprint 4 | M3, M6 | Hoàn thành | `StockReportService.java` | `StockReportsPage.tsx` | Đạt ✅ |
| **FR-REP-03** | Cảnh báo tồn kho thấp dưới ngưỡng đặt lại | Sprint 4 | M4, M6 | Hoàn thành | `StockAlertService.java` | `DashboardPage.tsx`, `AppLayout.tsx` | Đạt ✅ |
| **FR-SIM-02** | WebSocket STOMP realtime theo tenant | Sprint 4 | M2, M6 | Hoàn thành | `WebSocketConfig.java`, `RealtimeEventListener` | `AppLayout.tsx` (STOMP + Âm thanh) | Đạt ✅ |
| **FR-SIM-03** | Tác vụ nền định kỳ chốt sổ và hủy đơn quá hạn | Sprint 4 | M5 | Hoàn thành | `ReservationExpirationJob`, `DailyReportJob` | `StockReportsPage.tsx` | Đạt ✅ |

