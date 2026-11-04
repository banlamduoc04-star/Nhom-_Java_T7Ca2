# KỊCH BẢN VÀ BÁO CÁO KIỂM THỬ CHẤP NHẬN (UAT) SPRINT 4
**Dự án:** OISM  
**Người thực hiện:** M1 – Dung Hoang (Lead / Scrum Master / QA)  
**Ngày thực hiện:** 04/11/2026  
**Môi trường:** Docker Compose / Local Dev (PostgreSQL 16, Spring Boot 3.3, React 19)

---

## 1. Kịch bản kiểm thử luồng đầu - cuối (End-to-End User Acceptance Test)

### Kịch bản 1: Nhập kho $\rightarrow$ Bán lẻ tại quầy POS $\rightarrow$ In hóa đơn $\rightarrow$ Cập nhật tồn kho
- **Bước 1:** Owner tạo phiếu nhập kho 50 chiếc Áo thun (SKU `TSHIRT-M`) với giá nhập 100.000 ₫ tại Chi nhánh 1. Xác nhận phiếu nhập.
  - *Kết quả mong đợi:* Tồn kho `on_hand` tăng lên 50, giá vốn `avg_cost` cập nhật 100.000 ₫, ghi 1 dòng sổ cái IN. (Đạt ✅)
- **Bước 2:** Thu ngân đăng nhập ứng dụng POS, quét mã vạch hoặc nhập mã `TSHIRT-M`. Thêm 2 chiếc vào giỏ.
  - *Kết quả mong đợi:* Giỏ hàng hiển thị 2 chiếc, đơn giá 150.000 ₫, tổng tiền 300.000 ₫. (Đạt ✅)
- **Bước 3:** Bấm F9 mở hộp thoại thanh toán, chọn "Tiền mặt", nhập tiền khách đưa 500.000 ₫.
  - *Kết quả mong đợi:* Hệ thống tự tính tiền thối 200.000 ₫. Bấm "Xác nhận & In bill". (Đạt ✅)
- **Bước 4:** Kiểm tra hóa đơn và cơ sở dữ liệu:
  - *Kết quả mong đợi:*
    - Hóa đơn 80mm xuất hiện đầy đủ thông tin mã đơn, thu ngân, chi tiết mặt hàng, tiền thối.
    - Đơn hàng chuyển sang `COMPLETED`.
    - Tồn kho `available` và `on_hand` của `TSHIRT-M` giảm xuống 48.
    - Sổ cái ghi nhận dòng OUT bán hàng kèm giá vốn 100.000 ₫. (Đạt ✅)

---

### Kịch bản 2: Bán vượt tồn kho & Cảnh báo tồn thấp (Low Stock Alert)
- **Bước 1:** Cấu hình SKU `TSHIRT-M` có ngưỡng đặt lại `reorder_point = 10`. Hiện tại kho còn 11 chiếc.
- **Bước 2:** Khách mua tại POS 2 chiếc (còn 9 chiếc khả dụng).
- **Bước 3:** Hoàn tất thanh toán đơn hàng.
  - *Kết quả mong đợi:*
    - Tồn kho khả dụng giảm còn 9 ($\le 10$).
    - Hệ thống tự động tạo bản ghi trong bảng `stock_alert` với trạng thái `OPEN`.
    - Admin Dashboard xuất hiện chuông thông báo và hiển thị cảnh báo đỏ tại danh sách cảnh báo. (Đạt ✅)
- **Bước 4:** Tạo và xác nhận phiếu nhập thêm 50 chiếc.
  - *Kết quả mong đợi:* Tồn kho tăng lên 59 (> 10), cảnh báo tự động chuyển trạng thái `RESOLVED` và đóng lại. (Đạt ✅)

---

### Kịch bản 3: Đơn hàng từ Sàn TMĐT $\rightarrow$ WebSocket Realtime $\rightarrow$ Tự động hủy đơn giữ quá hạn
- **Bước 1:** Giả lập Webhook từ Shopee gửi đơn hàng mua 1 chiếc `TSHIRT-M`.
- **Bước 2:** Hệ thống tiếp nhận, tạo đơn `RESERVED` với thời hạn giữ `reserved_until = NOW() + 30 phút`.
  - *Kết quả mong đợi:* Admin Dashboard nhận tin qua WebSocket STOMP, tự động phát chuông kêu và hiển thị popup thông báo đơn hàng mới mà không cần tải lại trang. (Đạt ✅)
- **Bước 3:** Kiểm tra tác vụ nền `ReservationExpirationJob`. Khi quá hạn giữ hàng mà đơn chưa được chuyển sang `CONFIRMED`:
  - *Kết quả mong đợi:* Tác vụ nền tự động hủy đơn (`CANCELLED`) và nhả số lượng `reserved`, khôi phục số lượng `available`. (Đạt ✅)

---

### Kịch bản 4: Báo cáo phân tích kinh doanh & Định giá tồn kho
- **Bước 1:** Vào trang Báo cáo tồn kho trên Admin.
  - *Kết quả mong đợi:*
    - Thẻ Định giá tồn kho tính đúng: $\text{Tổng giá trị} = \sum (\text{on\_hand} \times \text{avg\_cost})$.
    - Thẻ Top bán chạy liệt kê chính xác các sản phẩm vừa bán tại POS và đơn sàn.
    - Danh sách hàng tồn chậm hiển thị các sản phẩm không phát sinh đơn bán trong 30 ngày. (Đạt ✅)
- **Bước 2:** Kiểm tra tác vụ chốt sổ ngày `DailyReportJob` hoặc bấm "Chốt sổ hôm qua ngay".
  - *Kết quả mong đợi:* Tạo bản ghi trong `daily_report` tổng hợp đầy đủ doanh thu, giá vốn COGS và lợi nhuận gộp. (Đạt ✅)

---

## 2. Bảng theo dõi và xử lý lỗi (Defect Tracking)

| ID | Module | Mô tả sự cố | Mức độ | Nguyên nhân | Biện pháp xử lý | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-S4-01 | Web | Lỗi ép kiểu `user.fullName()` trong `PosController` | Trung bình | `AuthenticatedUser` lưu email thay vì fullName | Dùng `user.email()` làm định danh thu ngân | Đã sửa ✅ |
| BUG-S4-02 | WebSocket | Header JWT bị thiếu prefix Bearer khi client gửi raw token | Thấp | Một số thư viện STOMP client gửi token không kèm tiền tố | Chuẩn hóa header trong `ChannelInterceptor` | Đã sửa ✅ |
| BUG-S4-03 | Frontend POS | In hóa đơn bị ẩn trên trình duyệt do CSS thiếu rule media print | Cao | `@media print` chưa set visibility cho container bill 80mm | Bổ sung class `.receipt-paper` hiển thị khi in | Đã sửa ✅ |

---

## 3. Kết luận nghiệm thu Sprint 4
Toàn bộ các tiêu chí nghiệm thu của Sprint 4 (mục 7 và 8 trong kế hoạch dự án) đã đạt 100%. Hệ thống sẵn sàng cho Sprint 5 (Đóng gói, kiểm thử hiệu năng và hoàn thiện tài liệu).
