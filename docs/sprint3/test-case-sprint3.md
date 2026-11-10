# BẢNG TEST CASE CHI TIẾT SPRINT 3
**Dự án:** OISM  
**QA Lead:** Dung Hoang  
**Sprint:** 3 · **Cập nhật:** 11/11/2026 – chạy lại trên nhánh Sprint 5, mỗi case có bằng chứng là test tự động hoặc lần chạy trên bản Docker

---

Cột "Bằng chứng" nêu test tự động trong `backend/src/test` (`*Test` = unit, `*IT` = tích hợp với PostgreSQL thật) hoặc collection Postman `OISM_Core_Auth_Inv_Ord_Pos_Sim`. Kết quả: `./mvnw verify` 115 unit + 59 tích hợp đạt; newman 244/244 assertion đạt.

| Mã TC | Phân hệ / Chức năng | Điều kiện & Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Bằng chứng | Trạng thái |
|---|---|---|---|---|---|---|
| **TC-ORD-01** | Tạo đơn nháp (Draft) | Chi nhánh active, SKU hợp lệ | `POST /api/v1/orders` với `items` | Đơn `DRAFT`, tổng tiền theo giá bán lẻ, tồn chưa đổi, lịch sử `null→DRAFT` | `OrderApiIT.createsDraftOrderWithTotalAndHistory` | **PASS** |
| **TC-ORD-02** | Giữ hàng cho đơn nháp | Đơn `DRAFT` | `POST /orders/{id}/reserve` | `RESERVED`, `reserved` tăng, `available` giảm, `reserved_until` = now + 30 phút | `OrderLifecycleIT.cancelReservedReleasesAndCancelConfirmedReturnsStockWithLedger`; Postman 5 | **PASS** |
| **TC-ORD-03** | Xác nhận đơn | Đơn `RESERVED` | `POST /orders/{id}/confirm` | `CONFIRMED`; `reserved` và `on_hand` giảm; sổ cái `OUT`/`SALE`; `cost_price` chụp từ `avg_cost` | `ReservationServiceTest.confirm_releasesReserved_capturesAvgCost_andPostsLedger`; Postman 5 (kiểm `costPrice > 0`) | **PASS** |
| **TC-ORD-04** | Hoàn tất đơn | Đơn `CONFIRMED` | `POST /orders/{id}/complete` | `COMPLETED`; hoàn tất đơn `DRAFT` → 409 `OD40901` | `OrderStateMachineTest`; Postman 5 | **PASS** |
| **TC-ORD-05** | Hủy đơn đang giữ | Đơn `RESERVED` | `POST /orders/{id}/cancel` kèm lý do | `CANCELLED`, nhả `reserved`; thiếu lý do → 400 `OD40001` | `OrderLifecycleIT.cancelReservedReleasesAndCancelConfirmedReturnsStockWithLedger`; Postman 5 | **PASS** |
| **TC-ORD-06** | Hủy đơn đã xác nhận | Đơn `CONFIRMED` | `POST /orders/{id}/cancel` kèm lý do | `CANCELLED`; `on_hand` tăng lại; sổ cái `IN`/`RETURN` với giá vốn đã chụp | `OrderLifecycleIT.cancelReservedReleasesAndCancelConfirmedReturnsStockWithLedger` | **PASS** |
| **TC-RSE-01** | Giữ hàng khi đủ | OnHand=20, Reserved=5, Available=15; đặt 10 | Giữ hàng | Thành công, Reserved=15, Available=5 | `ReservationServiceTest.reserve_success_whenAvailableIsSufficient` | **PASS** |
| **TC-RSE-02** | Chặn bán vượt | Available=5; đặt 10 | Giữ hàng | 409 `IV40901`, hoàn tác toàn bộ đơn | `ReservationServiceTest.reserve_throwsIV40901_whenAvailableIsInsufficient`; `TransactionRollbackIT` | **PASS** |
| **TC-RSE-03** | Không deadlock khi khóa nhiều SKU | 20 đơn đồng thời, nửa đặt (A, B), nửa đặt (B, A) | Gửi đồng thời qua webhook | Cả 20 đơn giữ hàng thành công (201), không lỗi 500/deadlock; `reserved` mỗi SKU = 20 | `OrderLifecycleIT.concurrentOrdersLockingSkusInOppositeOrderNeverDeadlock` | **PASS** |
| **TC-RSE-04** | Tự hủy đơn giữ quá hạn | Đơn `RESERVED` đã quá `reserved_until` | Tác vụ nền `cancelExpiredReservations` (mỗi 60 giây) | `CANCELLED`, lý do "Quá hạn giữ hàng", nhả `reserved` | `OrderLifecycleIT.expiredReservationIsCancelledAutomaticallyAndReleased`; Docker: đơn `DH-20261110-1344` tự hủy sau 105 giây (UAT Sprint 4, kịch bản 3) | **PASS** |
| **TC-TRN-01** | Tạo phiếu chuyển nháp | Chi nhánh A (nguồn), B (đích) | `POST /api/v1/transfers` | Phiếu `DRAFT`, tồn 2 kho chưa đổi; trùng chi nhánh → 400 `IV40001` | `StockTransferServiceTest.create_success_whenValidBranchesAndSkus`, `create_fails_whenSameBranches` | **PASS** |
| **TC-TRN-02** | Xuất chuyển kho | Phiếu `DRAFT`, kho A đủ tồn | `POST /transfers/{id}/dispatch` | `IN_TRANSIT`; kho A giảm, sổ cái `OUT`/`TRANSFER_OUT`; vượt tồn → 409 `IV40901` | `StockTransferServiceTest.dispatch_movesToInTransit_andPostsLedgerOut`; Postman 4 | **PASS** |
| **TC-TRN-03** | Nhận chuyển kho | Phiếu `IN_TRANSIT` | `POST /transfers/{id}/receive` | `RECEIVED`; kho B tăng, sổ cái `IN`/`TRANSFER_IN` | `StockTransferServiceTest.receive_movesToReceived_andPostsLedgerIn`; Postman 4 | **PASS** |
| **TC-TRN-04** | Hủy phiếu chuyển | Phiếu `DRAFT` / `IN_TRANSIT` | `POST /transfers/{id}/cancel` | `DRAFT` → `CANCELLED`, tồn không đổi; phiếu đã xuất → 409 `IV40902` | `OrderLifecycleIT.draftTransferCanBeCancelledButDispatchedCannot` | **PASS** |
| **TC-STK-01** | Mở phiên kiểm kê | Chi nhánh có tồn | `POST /api/v1/stocktakes` | `OPEN`, chụp `on_hand` vào `systemQuantity` | `StocktakeServiceTest.create_capturesSystemQuantityFromOnHand`; Postman 4 | **PASS** |
| **TC-STK-02** | Ghi số đếm | systemQuantity=18, đếm 17 | `PUT /stocktakes/{id}/counts` `{counts:[{skuId, countedQuantity}]}` | countedQuantity=17, difference=-1 | `StocktakeServiceTest.recordCounts_updatesCountedAndDifference`; Postman 4 | **PASS** |
| **TC-STK-03** | Duyệt kiểm kê | difference=-1; người duyệt là Owner | `POST /stocktakes/{id}/approve` | `APPROVED`, sổ cái `OUT`/`ADJUST` 1 cái; Staff duyệt → 403 `AU40301`; sửa sau duyệt → 409 `IV40902` | `StocktakeServiceTest.approve_createsAdjustLedgerEntry_forVariance`; Postman 4 | **PASS** |
| **TC-RPT-01** | Báo cáo lợi nhuận gộp | 2 đơn POS × 2 cái, giá bán 150.000, giá vốn 100.000 | `GET /api/v1/reports/profit` | Doanh thu 600.000; giá vốn 400.000; lợi nhuận gộp 200.000; biên 0,3333; đơn Draft/Reserved không tính | `ProfitReportIT.profitReportMatchesHandCalculationAndFilters`; Docker UAT Sprint 4 kịch bản 4 | **PASS** |
| **TC-RPT-02** | Xuất CSV | Có đơn đã bán | `GET /reports/profit/export` | `text/csv` UTF-8 có BOM, chi tiết từng dòng hàng và dòng TỔNG CỘNG, số dùng dấu chấm | `ProfitReportServiceTest.csvHasDotDecimalsAndTotals`; `ProfitReportIT` | **PASS** |
| **TC-SIM-01** | Flash Sale qua Simulator | SKU còn 1; 50 đơn × 1 cái (NFR-PERF-02) | `POST /api/v1/simulator/orders` | 1 đơn giữ hàng, 49 lỗi `IV40901`, `available` = 0 | `FlashSaleIT.simulator_50ConcurrentOrders_1Stock_exactlyOneReserved`; Docker: xem `kich-ban-flash-sale.md` | **PASS** |
| **TC-WHK-01** | Webhook sàn | Payload Shopee/TikTok Shop, header `X-Api-Key` | `POST /api/v1/webhooks/shopee` | Tạo đơn `RESERVED` cho đúng tenant của key; trùng mã đơn ngoài → 409 `OD40902`; thiếu/sai key → 401 `OD40101` | `WebhookIT` (7 case) | **PASS** |
