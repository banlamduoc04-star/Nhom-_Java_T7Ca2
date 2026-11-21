# Dữ liệu vận hành mẫu

Bộ sinh này mô phỏng 3 cửa hàng hoạt động liên tục từ ngày mở đến thời điểm chạy script, thay vì chèn số liệu cố định:

| Cửa hàng | Ngành | Chi nhánh | Kênh bán |
|---|---|---|---|
| Giày Dép Hưng Thịnh (Hà Nội) | Giày dép | 2 cửa hàng + kho Long Biên | POS, Shopee, TikTok Shop, đơn tay |
| Thời trang Mộc Lan (TP.HCM) | Quần áo | 2 cửa hàng + kho Gò Vấp | POS, Shopee, TikTok Shop, đơn tay |
| Tạp hóa Phúc An (Đà Nẵng) | Tạp hóa | 1 cửa hàng | POS, đơn gọi điện |

Mô phỏng gồm những gì:

- **Bán hàng:** số đơn mỗi ngày ngẫu nhiên. Cuối tuần, ngày lương, 20/10 và các đợt sale 10/10, 11/11 đông hơn; ngày mưa thì vắng. Giờ mua tập trung vào trưa và tối. Khách chọn size theo phân bố thực tế, hết size thì đổi size khác hoặc bỏ không mua. Thu ngân chia theo ca, có người nghỉ việc giữa chừng và có người mới vào làm.
- **Đơn sàn và đơn tay:** giữ hàng theo `reservation_minutes`. Có đơn quá hạn tự hủy, đơn khách hủy, đơn giao thất bại bị hoàn kho, đơn đang giao và đơn nháp.
- **Kho:** nhập hàng khi tồn xuống thấp, gom theo nhà cung cấp, giá nhập dao động. Giá vốn bình quân gia quyền tính đúng như `CostCalculator`. Hàng chuyển từ kho ra cửa hàng qua các trạng thái nháp, đang chuyển, đã nhận, hủy. Cuối tháng kiểm kê, có lệch hao hụt.
- **Sổ cái, tồn kho, cảnh báo tồn thấp, chốt sổ ngày** được tính khớp với nghiệp vụ backend.

## Chạy

Cần Python 3 và gói `bcrypt` (`pip install bcrypt`). Stack Docker phải đang chạy.

```powershell
.\scripts\du-lieu-mau\nap-du-lieu.ps1
```

> **Lưu ý:** script **xóa sạch toàn bộ dữ liệu nghiệp vụ** đang có trong `oism_db` (giữ lại lịch sử Flyway) rồi mới nạp dữ liệu mới. Tất cả chạy trong một transaction: nếu lỗi thì dữ liệu cũ vẫn còn nguyên.

Mỗi lần chạy sinh ra một bộ dữ liệu khác. Truyền `-Seed <số>` để tạo lại đúng bộ dữ liệu đã có. Email các tài khoản luôn giữ nguyên, mật khẩu chung là `admin123` (chỉ dùng cho dev/demo). Danh sách tài khoản cũng được ghi vào `tai-khoan.local.md` (file này nằm trong `.gitignore`).

Các khóa API sàn (`channel_credential`) chỉ lưu bản băm ngẫu nhiên. Muốn thử webhook thì Owner vào Admin sinh khóa mới.
