# KẾ HOẠCH KIỂM THỬ SPRINT 3 (TEST PLAN V3)
**Dự án:** Omnichannel Inventory & Sales Management (OISM)  
**Tác giả:** Dung Hoang (QA / Test Lead)  
**Sprint:** 3 (Đơn hàng đa kênh, Giữ hàng Flash Sale, Chuyển kho, Kiểm kê, Báo cáo lợi nhuận)  
**Ngày lập:** 04/11/2026  

---

## 1. Mục tiêu kiểm thử
Xác nhận chất lượng, tính đúng đắn và độ bền chịu tải của toàn bộ chức năng Sprint 3:
1. **Quản lý đơn hàng (ORD):** Tạo đơn nháp (Draft), chuyển trạng thái theo OrderStateMachine.
2. **Cơ chế Giữ hàng & Chống Oversell (RSE):** Thuật toán khóa dòng `SELECT ... FOR UPDATE` sắp xếp tăng dần theo `sku_id` tránh deadlock; giữ hàng 15 phút, tự động nhả tồn khi hết hạn hoặc khi đơn bị hủy.
3. **Chụp giá vốn (COST-02):** Chụp giá vốn bình quân gia quyền hiện hành của SKU tại thời điểm đơn hàng chuyển sang `CONFIRMED`.
4. **Chuyển kho liên chi nhánh (TRN):** Quy trình 2 bước: Xuất kho chuyển đi (`OUT_TRANSFER`) và Nhập kho chi nhánh đích (`IN_TRANSFER`).
5. **Kiểm kê kho & Cân chỉnh tồn (STK):** Chụp tồn hệ thống (`OPEN`), ghi nhận số lượng thực tế đếm (`COUNTED`), duyệt và tự động sinh bút toán sổ cái điều chỉnh (`ADJUST`).
6. **Báo cáo Lợi nhuận gộp (RPT):** Tính toán Doanh thu, Giá vốn (COGS), Lợi nhuận gộp và Tỷ suất biên (%) theo ngày, chi nhánh, kênh bán và SKU; xuất CSV.
7. **Kịch bản Flash Sale đa luồng:** Kiểm thử chịu tải đồng thời từ nhiều luồng độc lập, chứng minh không bao giờ bán âm kho hay deadlock.

---

## 2. Phạm vi kiểm thử

### 2.1 Trong phạm vi (In-Scope)
- **Backend APIs:**
  - `/api/v1/orders` (CRUD, reserve, confirm, complete, cancel).
  - `/api/v1/transfers` (dispatch, receive, cancel).
  - `/api/v1/stocktakes` (counts, approve).
  - `/api/v1/reports/profit` và `/export` (CSV).
  - `/api/v1/simulator/orders` và `/api/v1/webhooks/{channel}`.
- **Frontend Admin & POS:**
  - Admin Web: Trang Đơn hàng, Chuyển kho, Kiểm kê kho, Báo cáo lợi nhuận, Giả lập Flash Sale / Webhook.
  - POS Web: Tra cứu tồn kho khả dụng thời gian thực, chặn thêm vượt hàng khả dụng.
- **Database & Hiệu năng:**
  - Migration Flyway V5: Các composite index `(tenant_id, created_at)` và `(tenant_id, sku_id)`.
  - Transaction Rollback khi giữ hàng thất bại giữa chừng.

### 2.2 Ngoài phạm vi (Out-of-Scope)
- Tích hợp cổng thanh toán thực tế VNPAY / VietQR (Sprint 4).
- In hóa đơn nhiệt POS 80mm ESC/POS vật lý (Sprint 4).

---

## 3. Tiêu chí hoàn thành (Exit Criteria)
- 100% Unit test & Architecture test (ArchUnit) pass (tối thiểu 75 tests).
- 0 lỗi vi phạm phụ thuộc tầng Clean Architecture.
- 100% Test case nghiệp vụ Sprint 3 đạt trạng thái PASS.
- Build Frontend Admin và Frontend POS không có lỗi TypeScript / lint.
