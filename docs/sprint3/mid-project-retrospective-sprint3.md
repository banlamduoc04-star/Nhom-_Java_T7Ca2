# BIÊN BẢN HỌP ĐÁNH GIÁ GIỮA KỲ (MID-PROJECT RETROSPECTIVE - SPRINT 3)
**Dự án:** OISM  
**Chủ trì:** Dung Hoang (Project Lead & QA)  
**Tham dự:** Toàn bộ thành viên dự án (M1, M2, M3, M4, M5, M6, M7)  
**Thời gian:** 04/11/2026  

---

## 1. Đánh giá Tiến độ Chung (Progress Evaluation)
- Dự án đã hoàn tất trọn vẹn 3 trên 5 Sprint theo đúng lộ trình kế hoạch.
- **Sprint 1:** Kiến trúc lõi đa chi nhánh, Authentication JWT, Quản lý Danh mục, Sản phẩm, SKU, Nhà cung cấp.
- **Sprint 2:** Quản lý kho, Phiếu nhập kho, Thuật toán Giá vốn bình quân gia quyền (MAC), Sổ cái kho bất biến (`inventory_transactions`), Barcode EAN-13, Màn hình POS cơ bản.
- **Sprint 3:** Vòng đời Đơn hàng đa kênh, Thuật toán giữ hàng Flash Sale chống Oversell và Deadlock, Chuyển kho liên chi nhánh, Kiểm kê cân chỉnh tồn, Báo cáo Lợi nhuận gộp, Bộ giả lập Webhook & Đua đơn.

---

## 2. Đánh giá theo Thành viên (Member Performance Review)

| Thành viên | Vai trò | Đóng góp trọng tâm Sprint 3 | Đánh giá |
|---|---|---|:---:|
| **Dung Hoang (M1)** | Lead & QA | Test plan, Test cases, Kịch bản Flash Sale, RTM v3, Biên bản Retro | **Xuất sắc** |
| **Nguyễn Đức Thuận (M2)** | Backend Senior | Audit @Transactional, Flyway V5 composite indexes, Transaction rollback test | **Xuất sắc** |
| **Trần Ngọc Lâm (M3)** | Backend Dev | ProfitReportService, Báo cáo Lợi nhuận, Export CSV, Test suite toán học | **Xuất sắc** |
| **Lê Quốc Khánh (M4)** | Backend Dev | Chuyển kho (2 bước), Kiểm kê kho cân chỉnh sổ cái, Chụp giá vốn | **Xuất sắc** |
| **Trương Quang Vinh (M5)** | Backend Dev | ReservationService, Khóa có thứ tự chống deadlock, Webhook & Simulator API | **Xuất sắc** |
| **Phạm Chấn Nguyên (M6)** | Frontend Admin | UI Đơn hàng (chuyển trạng thái, in phiếu), Chuyển kho, Kiểm kê, Báo cáo lợi nhuận | **Xuất sắc** |
| **Nghĩa Chu (M7)** | Frontend POS | POS tra cứu tồn khả dụng realtime, Giao diện Simulator Webhook đa kênh | **Xuất sắc** |

---

## 3. Điểm làm tốt (What Went Well)
1. **Tuân thủ Clean Architecture tuyệt đối:** Không để tầng Web hoặc Application phụ thuộc vào Infrastructure; 100% test ArchUnit vượt qua.
2. **Cơ chế chống Deadlock xuất sắc:** Sắp xếp `sku_id` tăng dần trước khi khóa dòng triệt tiêu hoàn toàn khả năng deadlock khi chịu tải Flash Sale đồng thời.
3. **Phối hợp Backend - Frontend nhịp nhàng:** Toàn bộ API đều có giao diện Admin Web và POS hoàn chỉnh, trực quan, hỗ trợ nghiệp vụ thực tế.
4. **Độ bao phủ Test cao:** Toàn dự án sở hữu 79 Unit Test & Architecture Test tự động hóa.

---

## 4. Điểm cần cải thiện & Mục tiêu Sprint 4 (Action Items for Sprint 4)
1. **Tích hợp Thanh toán:** Chuẩn bị kết nối VNPAY / VietQR sinh mã QR động tại màn hình POS.
2. **In hóa đơn bán lẻ:** Tích hợp giao thức in nhiệt ESC/POS 80mm cho máy in hóa đơn quầy thu ngân.
3. **Cảnh báo tồn kho dưới định mức (Reorder Point Alert):** Tự động gửi thông báo khi tồn kho khả dụng thấp hơn ngưỡng tái đặt hàng.
