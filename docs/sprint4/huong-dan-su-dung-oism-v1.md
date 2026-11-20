# HƯỚNG DẪN SỬ DỤNG HỆ THỐNG OISM (V1)
**Dành cho:** Chủ cửa hàng (Owner), Quản lý/Nhân viên kho (Staff), Thu ngân (Cashier)  
**Phiên bản:** v1.0 (Sprint 4 Release)  

---

## PHẦN I: HƯỚNG DẪN DÀNH CHO ỨNG DỤNG BÁN HÀNG TẠI QUẦY (POS)

Ứng dụng POS hỗ trợ chạy trực tiếp trên trình duyệt hoặc cài đặt như ứng dụng độc lập (PWA) trên máy tính bảng/máy bán hàng POS chuyên dụng.

### 1. Đăng nhập ứng dụng POS
- Truy cập địa chỉ POS (mặc định: `http://localhost:3001` hoặc `http://localhost:5173`).
- Nhập thông tin đăng nhập (Email hoặc Số điện thoại + Mật khẩu).
- *Lưu ý:* Tài khoản thu ngân bắt buộc phải được chủ cửa hàng gán cho một chi nhánh cụ thể để có thể bán hàng và trừ tồn kho đúng chi nhánh đó.

### 2. Các thao tác bán hàng nhanh
- **Tìm kiếm sản phẩm:**
  - Bấm phím tắt **`F2`** để con trỏ nhảy ngay vào ô tìm kiếm.
  - Quét mã vạch sản phẩm bằng máy quét: Hệ thống tự động nhận diện và thêm ngay 1 sản phẩm vào giỏ hàng.
  - Quét lại mã vạch một lần nữa: Số lượng trong giỏ hàng sẽ tự động tăng thêm 1.
  - Nhập tên hoặc mã SKU: Hệ thống hiển thị danh sách gợi ý kèm giá bán và số lượng còn khả dụng trong kho.
- **Điều chỉnh giỏ hàng:**
  - Bấm nút **`+`** hoặc **`-`** để tăng giảm số lượng.
  - Hệ thống kiểm tra số lượng khả dụng theo thời gian thực và ngăn chặn vượt tồn kho.
  - Bấm nút **`✕`** để xóa một dòng hàng.
  - Bấm **`Esc`** để hủy toàn bộ giỏ hàng (hệ thống có hộp thoại xác nhận).

### 3. Thanh toán đơn hàng và In hóa đơn
- Bấm phím **`F9`** hoặc bấm nút **"Thanh toán (F9)"** ở góc dưới giỏ hàng để mở cửa sổ thanh toán.
- **Thanh toán Tiền mặt (Cash):**
  - Màn hình hiển thị tổng tiền cần thanh toán.
  - Thu ngân nhập số tiền khách đưa hoặc bấm nhanh các nút gợi ý (`100k`, `200k`, `500k`, `+50k`, `Đúng tiền`).
  - Hệ thống tự động tính số tiền thừa (tiền thối) cần trả lại cho khách.
- **Thanh toán Chuyển khoản QR (QR Code):**
  - Bấm chọn tab "📱 Chuyển khoản QR".
  - Màn hình hiển thị mã QR (sinh ngay trên máy, không gửi dữ liệu ra ngoài) chứa thông tin đơn hàng và số tiền.
  - Khách hàng quét mã qua ứng dụng ngân hàng/ví điện tử. Sau khi xác nhận tiền đã vào tài khoản, thu ngân bấm xác nhận.
- **Hoàn tất và In hóa đơn:**
  - Bấm **"Xác nhận & In bill (F9)"**. Đơn hàng được xử lý tức thì trong 1 giao dịch và xuất hóa đơn chuẩn 80mm.
  - Bấm nút **"🖨️ In hóa đơn"** (hoặc `Ctrl + P`) để in ra máy in nhiệt khổ 80mm.
  - Bấm **`Esc`** hoặc nút **"Đơn mới"** để bắt đầu lượt bán hàng tiếp theo.

---

## PHẦN II: HƯỚNG DẪN DÀNH CHO CỔNG QUẢN TRỊ (ADMIN DASHBOARD)

Địa chỉ truy cập: `http://localhost:3000` (hoặc cổng dev).

### 1. Trang Tổng quan (Dashboard)
- **Thẻ chỉ số tài chính:** Hiển thị tổng doanh thu thuần, lợi nhuận gộp toàn cửa hàng theo thời gian thực.
- **Theo dõi đơn hàng:** Thống kê số lượng đơn đang chờ duyệt, đơn đang giữ hàng, đơn hoàn tất.
- **Khu vực Cảnh báo tồn thấp:** Liệt kê các SKU có tồn khả dụng $\le$ ngưỡng đặt lại (`reorder_point`). Bấm nút "Đóng" khi đã xử lý hoặc bấm "Xem tồn kho" để lập phiếu nhập.
- **Thông báo Realtime:** Khi có đơn hàng mới phát sinh từ quầy POS hoặc sàn thương mại điện tử, hệ thống tự động phát âm thanh chuông báo và hiển thị popup thông báo góc phải màn hình.

### 2. Quản lý Đơn hàng & Chống bán vượt
- Vào mục **"Đơn hàng"**:
  - Xem danh sách đơn từ các kênh (POS, Thủ công, Shopee, TikTok Shop).
  - Lọc theo trạng thái: `DRAFT`, `RESERVED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`.
  - Thao tác: Giữ hàng (`Reserve`), Duyệt đơn trừ kho (`Confirm`), Hoàn tất (`Complete`), Hủy đơn có lý do (`Cancel`).

### 3. Nghiệp vụ Kho & Phiếu kho
- **Phiếu nhập kho:** Tạo phiếu nhập từ Nhà cung cấp, thêm danh sách SKU và giá nhập. Khi bấm "Xác nhận", kho sẽ tăng tồn và tự động tính lại giá vốn bình quân gia quyền.
- **Chuyển kho:** Tạo phiếu chuyển hàng giữa các chi nhánh (Xuất kho đi $\rightarrow$ Nhận kho đến).
- **Kiểm kê kho:** Tạo phiên kiểm kê, nhập số đếm thực tế, hệ thống tự động tính chênh lệch và sinh bút toán điều chỉnh khi duyệt.
- **Tồn kho & Sổ cái:** Tra cứu số lượng tồn thực tế (`on_hand`), hàng đang giữ (`reserved`) và hàng khả dụng (`available`); tra cứu toàn bộ lịch sử sổ cái chỉ ghi thêm.

### 4. Báo cáo phân tích kinh doanh
- **Báo cáo tồn kho & hàng bán** (Chủ cửa hàng và Nhân viên; tab chốt sổ ngày chỉ Chủ cửa hàng):
  - *Định giá tồn kho:* Tính tổng giá trị vốn lưu kho theo từng chi nhánh hoặc toàn bộ chuỗi.
  - *Top bán chạy:* Xếp hạng các mặt hàng bán chạy nhất theo số lượng và doanh thu trong 7/30/90 ngày.
  - *Hàng tồn chậm:* Liệt kê các SKU còn tồn đọng trong kho nhưng không phát sinh bất kỳ đơn bán nào trong N ngày qua.
  - *Chốt sổ ngày:* Xem doanh thu, giá vốn, lãi gộp theo từng ngày × chi nhánh × kênh; có nút chủ động chốt sổ ngay.
- **Báo cáo lợi nhuận:** Phân tích doanh thu, giá vốn COGS và biên lợi nhuận gộp, xuất file báo cáo CSV.

### 5. Cài đặt hệ thống & Phân quyền
- **Chi nhánh & Kho:** Thêm mới cửa hàng hoặc kho lưu trữ, ẩn/hiện chi nhánh.
- **Quản lý nhân viên:** Tạo tài khoản nhân viên (Staff) hoặc thu ngân (Cashier), gán chi nhánh làm việc, khóa tài khoản khi nghỉ việc, đặt lại mật khẩu.
- **API key webhook sàn:** vào **Cài đặt > API key webhook sàn**, bấm **Sinh key** cho Shopee hoặc TikTok Shop. Key chỉ hiện **một lần**, hãy sao chép ngay và cấu hình ở phía sàn (header `X-Api-Key`). Sinh lại key thì key cũ hết hiệu lực. Màn **Giả lập & Webhook** cũng cần nhập key này để gửi đơn thử.
- **Cài đặt thời hạn giữ hàng:** Cấu hình số phút giữ hàng tối đa cho các đơn online trước khi hệ thống tự động hủy đơn và nhả tồn kho (mặc định 30 phút).

