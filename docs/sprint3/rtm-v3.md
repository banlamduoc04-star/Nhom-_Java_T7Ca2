# MA TRẬN TRUY XUẤT YÊU CẦU PHIÊN BẢN 3 (RTM V3)
**Dự án:** OISM  
**QA Lead:** Dung Hoang  
**Ngày cập nhật:** 04/11/2026  

---

| Mã Yêu Cầu | Tên Yêu Cầu Nghiệp Vụ | Sprint | Thành phần Backend | Thành phần Frontend | Test Case | Kết quả |
|---|---|:---:|---|---|---|:---:|
| **FR-ORD-01** | Tạo đơn hàng bán lẻ / đa kênh | 3 | `OrderService.createDraft` | `OrdersPage.tsx` | TC-ORD-01 | PASS |
| **FR-ORD-02** | Chuyển trạng thái đơn hàng | 3 | `OrderService.moveTo` | `OrdersPage.tsx` | TC-ORD-02..04 | PASS |
| **FR-ORD-03** | Hủy đơn & hoàn trả tồn kho | 3 | `OrderService.cancel` | `OrdersPage.tsx` | TC-ORD-05..06 | PASS |
| **FR-RSE-01** | Giữ hàng trước khi thanh toán | 3 | `ReservationService.reserve` | `OrdersPage.tsx` | TC-RSE-01 | PASS |
| **FR-RSE-02** | Chặn bán vượt tồn khả dụng | 3 | `ReservationService` (IV40901) | `SaleScreen.tsx` | TC-RSE-02 | PASS |
| **FR-RSE-03** | Chống Deadlock sắp xếp SKU | 3 | `ReservationService` (sort skuId) | `SimulatorPage.tsx` | TC-RSE-03 | PASS |
| **FR-RSE-04** | Tự động hủy đơn hết hạn giữ | 3 | `OrderService.cancelExpired` | Backend Scheduler | TC-RSE-04 | PASS |
| **FR-COST-02**| Chụp giá vốn đơn hàng xuất | 3 | `ReservationService.confirm` | `OrdersPage.tsx` | TC-ORD-03 | PASS |
| **FR-TRN-01** | Tạo phiếu chuyển kho 2 chi nhánh | 3 | `StockTransferService.create` | `TransfersPage.tsx` | TC-TRN-01 | PASS |
| **FR-TRN-02** | Xuất chuyển kho chi nhánh nguồn | 3 | `StockTransferService.dispatch`| `TransfersPage.tsx` | TC-TRN-02 | PASS |
| **FR-TRN-03** | Nhận chuyển kho chi nhánh đích | 3 | `StockTransferService.receive` | `TransfersPage.tsx` | TC-TRN-03 | PASS |
| **FR-STK-01** | Tạo đợt kiểm kê & chụp tồn | 3 | `StocktakeService.create` | `StocktakesPage.tsx` | TC-STK-01 | PASS |
| **FR-STK-02** | Ghi nhận số lượng kiểm đếm | 3 | `StocktakeService.recordCounts` | `StocktakesPage.tsx` | TC-STK-02 | PASS |
| **FR-STK-03** | Phê duyệt cân chỉnh tồn sổ cái | 3 | `StocktakeService.approve` | `StocktakesPage.tsx` | TC-STK-03 | PASS |
| **FR-RPT-01** | Báo cáo Lợi nhuận gộp theo bộ lọc| 3 | `ProfitReportService.calculate`| `ProfitReportPage.tsx` | TC-RPT-01 | PASS |
| **FR-RPT-02** | Xuất báo cáo CSV | 3 | `ProfitReportService.exportCsv`| `ProfitReportPage.tsx` | TC-RPT-02 | PASS |
| **NFR-CONC-01**| Chống bán quá tồn đa luồng | 3 | `SimulatorController` | `SimulatorPage.tsx` | TC-SIM-01 | PASS |
