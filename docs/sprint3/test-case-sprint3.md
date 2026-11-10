# BẢNG TEST CASE CHI TIẾT SPRINT 3
**Dự án:** OISM  
**QA Lead:** Dung Hoang  
**Sprint:** 3  

---

| Mã TC | Phân hệ / Chức năng | Điều kiện & Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Trạng thái |
|---|---|---|---|---|---|
| **TC-ORD-01** | Tạo đơn hàng nháp (Draft) | Branch active, 2 SKU hợp lệ | Gọi POST `/api/v1/orders` với danh sách lines | Đơn hàng tạo ở trạng thái DRAFT, tồn kho chưa đổi | **PASS** |
| **TC-ORD-02** | Giữ hàng cho đơn nháp | Đơn hàng ở trạng thái DRAFT | Gọi POST `/api/v1/orders/{id}/reserve` | Status chuyển sang RESERVED, reserved tăng, available giảm tương ứng | **PASS** |
| **TC-ORD-03** | Xác nhận đơn hàng (Confirm) | Đơn ở trạng thái RESERVED | Gọi POST `/api/v1/orders/{id}/confirm` | Status sang CONFIRMED, reserved giảm, onHand giảm, ghi sổ OUT_SALE, cost_price được chụp từ avg_cost | **PASS** |
| **TC-ORD-04** | Hoàn tất đơn hàng | Đơn ở trạng thái CONFIRMED | Gọi POST `/api/v1/orders/{id}/complete` | Status sang COMPLETED | **PASS** |
| **TC-ORD-05** | Hủy đơn khi đang giữ hàng | Đơn ở trạng thái RESERVED | Gọi POST `/api/v1/orders/{id}/cancel` kèm lý do | Status sang CANCELLED, reserved được nhả lại kho, available phục hồi | **PASS** |
| **TC-ORD-06** | Hủy đơn sau khi đã xác nhận | Đơn ở trạng thái CONFIRMED | Gọi POST `/api/v1/orders/{id}/cancel` kèm lý do | Status sang CANCELLED, ghi sổ IN_RETURN hoàn lại on_hand | **PASS** |
| **TC-RSE-01** | Giữ hàng thành công khi đủ hàng | OnHand=20, Reserved=5, Available=15. Đặt 10 | Gọi createAndReserve | Thành công, Reserved=15, Available=5 | **PASS** |
| **TC-RSE-02** | Chặn bán vượt tồn khả dụng | Available=5. Đặt mua 10 | Gọi createAndReserve | Bắn lỗi `IV40901` (Không đủ hàng khả dụng), rollback toàn bộ đơn | **PASS** |
| **TC-RSE-03** | Chống Deadlock sắp xếp SKU | 2 luồng: T1 mua (SKU_A, SKU_B); T2 mua (SKU_B, SKU_A) | Thực thi đồng thời 2 luồng | Hệ thống luôn khóa theo thứ tự sku_id tăng dần, 0 deadlock | **PASS** |
| **TC-RSE-04** | Tự động hủy đơn quá hạn giữ hàng | Đơn RESERVED quá hạn 15 phút | Gọi cancelExpiredReservations | Đơn chuyển sang CANCELLED, tự động nhả reserved | **PASS** |
| **TC-TRN-01** | Tạo phiếu chuyển kho nháp | Chi nhánh A (nguồn), Chi nhánh B (đích) | Gọi POST `/api/v1/transfers` | Phiếu DRAFT, tồn cả 2 kho chưa đổi | **PASS** |
| **TC-TRN-02** | Xuất chuyển kho (Dispatch) | Phiếu DRAFT, kho A đủ tồn | Gọi POST `/api/v1/transfers/{id}/dispatch` | Trạng thái DISPATCHED, kho A giảm onHand ghi sổ `OUT_TRANSFER` | **PASS** |
| **TC-TRN-03** | Nhận chuyển kho (Receive) | Phiếu DISPATCHED | Gọi POST `/api/v1/transfers/{id}/receive` | Trạng thái RECEIVED, kho B tăng onHand ghi sổ `IN_TRANSFER` | **PASS** |
| **TC-TRN-04** | Hủy phiếu chuyển khi đang nháp | Phiếu DRAFT | Gọi POST `/api/v1/transfers/{id}/cancel` | Phiếu sang CANCELLED | **PASS** |
| **TC-STK-01** | Tạo đợt kiểm kê | Chi nhánh STORE-1 | Gọi POST `/api/v1/stocktakes` | Tạo OPEN, chụp tự động onHand hiện hành vào systemQty | **PASS** |
| **TC-STK-02** | Ghi nhận số đếm thực tế | SystemQty=20, thực tế đếm 18 | Gọi POST `/api/v1/stocktakes/{id}/counts` | CountedQty=18, Difference=-2 | **PASS** |
| **TC-STK-03** | Duyệt kiểm kê cân chỉnh tồn | Difference=-2 (thiếu) | Gọi POST `/api/v1/stocktakes/{id}/approve` | Trạng thái APPROVED, ghi sổ OUT_ADJUST số lượng 2 vào kho | **PASS** |
| **TC-RPT-01** | Tính toán báo cáo lợi nhuận | Đơn bán 2 cái giá bán 150k, giá vốn chụp 100k | Gọi GET `/api/v1/reports/profit` | Revenue=300k, COGS=200k, GrossProfit=100k, Margin=33.3% | **PASS** |
| **TC-RPT-02** | Xuất báo cáo CSV | Đã có dữ liệu bán hàng | Gọi GET `/api/v1/reports/profit/export` | Trả về stream text/csv UTF-8 đầy đủ các cột | **PASS** |
| **TC-SIM-01** | Mô phỏng Flash Sale 20 luồng | Tồn Available=10. 20 đơn x 1 | Gọi POST `/api/v1/simulator/orders` | 10 đơn thành công, 10 đơn bị từ chối; Tồn kho cuối cùng = 0 (không âm) | **PASS** |
| **TC-WHK-01** | Nhận Webhook sàn TMĐT | Payload từ Shopee, TikTok Shop | Gọi POST `/api/v1/webhooks/shopee` | Đơn hàng được tạo và giữ hàng tự động, chống trùng lặp externalOrderId | **PASS** |
