# KỊCH BẢN VÀ BÁO CÁO KIỂM THỬ CHẤP NHẬN (UAT) SPRINT 4
**Dự án:** OISM (Omnichannel Inventory and Sales Management System)  
**Người thực hiện:** M1 – Dung Hoang (Lead / Scrum Master / QA)  
**Ngày thực hiện:** 04/11/2026 · **Chạy lại:** 11/11/2026 (nhánh Sprint 5, sau kế hoạch sửa lỗi Sprint 5)  
**Môi trường:** bản Docker `docker compose --profile frontend up -d --build` (PostgreSQL 16, Spring Boot 3.3, React 19). Số liệu lấy từ lần chạy lại lúc 2026-11-10T20:12:48+00:00 (UTC); phần giao diện được kiểm bằng E2E Playwright (`e2e/tests`).

> **Ghi chú:** Bản báo cáo ngày 04/11/2026 ghi "Đạt" cho kịch bản 2 (bước 4: cảnh báo tự đóng khi nhập hàng) và kịch bản 3 (bước 2: popup đơn sàn trên Admin). Rà soát ngày 10/11/2026 cho thấy hai bước này **chưa chạy được** khi đó: phiếu nhập không đánh giá lại cảnh báo, đơn webhook không phát sự kiện, Admin kết nối WebSocket sai địa chỉ. Đã sửa ở Giai đoạn 2 (`docs/sprint5/ke-hoach-sua-loi-sprint5.md`); kết quả bên dưới là của lần chạy lại sau khi sửa.

---

## 1. Kịch bản kiểm thử đầu – cuối

### Kịch bản 1: Nhập kho → Bán tại quầy POS → In hóa đơn → Cập nhật tồn kho
| Bước | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|
| Owner nhập 50 `TSHIRT-M` giá 100.000 ₫, xác nhận phiếu | `on_hand` = 50; giá vốn 100.000 ₫; 1 dòng sổ cái IN | Phiếu `CONFIRMED`; `on_hand` = 50; giá vốn 100.000 ₫; sổ cái `IN`/`PURCHASE` 50 cái, `balance_after` 50 | ✅ |
| Thu ngân quét `TSHIRT-M`, thêm 2 cái, F9, tiền mặt 500.000 ₫ | Tổng 300.000 ₫, tiền thối 200.000 ₫ | Đơn `POS-261110-7050` `COMPLETED`; tổng 300.000 ₫; khách đưa 500.000 ₫; thối 200.000 ₫ | ✅ |
| Hóa đơn 80 mm và hộp thoại in | Hóa đơn hiện, hộp thoại in tự mở | E2E `pos.spec.ts` (máy tính, tablet, điện thoại): hóa đơn hiện, `window.print` được gọi đúng 1 lần mà không cần bấm | ✅ |
| Tồn kho và sổ cái sau bán | `on_hand` 48; sổ cái OUT/SALE giá vốn 100.000 ₫ | `on_hand` = 48, `reserved` = 0; sổ cái `OUT`/`SALE` 2 cái, giá vốn 100.000 ₫, `balance_after` 48 | ✅ |

### Kịch bản 2: Cảnh báo tồn thấp (FR-REP-03)
| Bước | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|
| SKU `TSHIRT-L` có `reorder_point` = 10, tồn 11 | Chưa có cảnh báo | `available` = 11; cảnh báo OPEN: 0 | ✅ |
| Bán tại POS 2 cái | `available` 9 ≤ 10 → 1 cảnh báo `OPEN` | `available` = 9; cảnh báo `OPEN` (available 9, ngưỡng 10) | ✅ |
| Nhập thêm 50 cái | `available` 59 > 10 → cảnh báo tự đóng `CLOSED` | `available` = 59; còn OPEN: 0; cảnh báo chuyển `CLOSED` lúc 2026-11-10T20:12:49.963476Z | ✅ |

Test tự động: `StockAlertIT` (đúng kịch bản này), `RealtimeIT` (cảnh báo được đẩy qua kênh `/alerts`).

### Kịch bản 3: Đơn sàn → WebSocket realtime → Tự hủy đơn giữ quá hạn
| Bước | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|
| Webhook Shopee (header `X-Api-Key` của cửa hàng) mua 1 `TSHIRT-M` | Đơn `RESERVED`, giữ 30 phút | Đơn `DH-20261110-8881` `RESERVED`, kênh SHOPEE, giữ 30 phút; `reserved` = 1, `available` = 47 | ✅ |
| Admin nhận popup + chuông, không tải lại trang | Popup "Có đơn hàng mới" | E2E `admin.spec.ts`: popup chứa mã đơn và kênh SHOPEE hiện trên trang Đơn hàng; `RealtimeIT`: client STOMP thật nhận đơn của tenant mình, tenant khác bị từ chối | ✅ |
| Đơn giữ quá hạn (đặt thời hạn 1 phút để chạy nhanh) | Tác vụ nền (mỗi 60 giây) hủy đơn, nhả `reserved` | Đơn `DH-20261110-1344` chuyển `CANCELLED` sau 105 giây, lý do "Quá hạn giữ hàng (tự động nhả tồn)"; `reserved` trở về 1 (chỉ còn đơn của bước 1) | ✅ |

### Kịch bản 4: Báo cáo phân tích và định giá tồn
| Bước | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|
| Giá trị tồn = Σ(on_hand × giá vốn) | (48 + 59) × 100.000 = 10.700.000 ₫ | 10.700.000 ₫, tổng on_hand 107 | ✅ |
| Top bán chạy (30 ngày) | Có 2 SKU vừa bán | `TSHIRT-M` 2 cái / 300.000 ₫, `TSHIRT-L` 2 cái / 300.000 ₫ | ✅ |
| Hàng tồn chậm (30 ngày) | Rỗng (cả 2 SKU vừa bán) | 0 dòng | ✅ |
| Lợi nhuận gộp (chỉ đơn Confirmed/Completed) | DT 600.000, GV 400.000, LN 200.000, biên 33,33%; đơn webhook đang giữ/đã hủy không tính | DT 600.000, GV 400.000, LN 200.000, biên 33.33%, 2 đơn | ✅ |
| Chốt sổ ngày cho hôm nay | 1 dòng chi nhánh × kênh POS | Chi nhánh chính × POS: 2 đơn, DT 600.000, GV 400.000, LN 200.000 | ✅ |

---

## 2. Bảng theo dõi và xử lý lỗi (Defect Tracking)

| ID | Module | Mô tả sự cố | Mức độ | Nguyên nhân | Biện pháp xử lý | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-S4-01 | Web | Lỗi ép kiểu `user.fullName()` trong `PosController` | Trung bình | `AuthenticatedUser` lưu email thay vì fullName | Dùng `user.email()` làm định danh thu ngân | Đã sửa ✅ |
| BUG-S4-02 | WebSocket | Header JWT bị thiếu prefix Bearer khi client gửi raw token | Thấp | Một số thư viện STOMP client gửi token không kèm tiền tố | Chuẩn hóa header trong `ChannelInterceptor` | Đã sửa ✅ |
| BUG-S4-03 | Frontend POS | In hóa đơn bị ẩn trên trình duyệt do CSS thiếu rule media print | Cao | `@media print` chưa set visibility cho container bill 80mm | Bổ sung class `.receipt-paper` hiển thị khi in | Đã sửa ✅ |
| BUG-S4-04 | Cảnh báo tồn | Nhập kho không đóng cảnh báo; đơn sàn không mở cảnh báo | Cao | Chỉ POS gọi kiểm tra cảnh báo | Kiểm tra sau mọi thay đổi tồn (sổ cái, giữ/nhả hàng) – Giai đoạn 2 | Đã sửa ✅ |
| BUG-S4-05 | Realtime | Đơn webhook/Simulator/tạo tay không hiện popup trên Admin | Cao | Chỉ POS phát sự kiện; Admin nối WebSocket vào host của chính trang Admin | Phát sự kiện ở mọi luồng tạo đơn; nối đúng backend – Giai đoạn 2 | Đã sửa ✅ |
| BUG-S4-06 | Bảo mật | Webhook nhận `X-Tenant-Id` từ client và khóa cứng; WebSocket không kiểm tra token | Nghiêm trọng | Thiếu `channel_credential`; interceptor bỏ qua lỗi | API key theo tenant, STOMP bắt buộc JWT – Giai đoạn 1 | Đã sửa ✅ |

## 3. Kết luận nghiệm thu Sprint 4
Lần chạy lại ngày 11/11/2026: cả 4 kịch bản đạt, đủ tiêu chí "Nghiệm thu Sprint 4" của kế hoạch: POS quét → F9 → nhập tiền → in hóa đơn trong tối đa 3 thao tác; tồn kho và báo cáo cập nhật ngay; đơn từ Simulator/webhook hiện popup không cần tải lại trang; đơn giữ quá hạn tự hủy và nhả hàng.
