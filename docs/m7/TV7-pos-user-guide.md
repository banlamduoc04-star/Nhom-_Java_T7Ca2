# M7 - Hướng dẫn sử dụng POS

## 1. Giới thiệu

Tài liệu hướng dẫn thu ngân sử dụng các chức năng cơ bản của ứng dụng POS trong hệ thống OISM.

## 2. Truy cập POS

1. Mở ứng dụng POS bằng trình duyệt được hỗ trợ.
2. Nhập thông tin tài khoản thu ngân.
3. Đăng nhập.
4. Kiểm tra màn hình bán hàng POS đã hiển thị.

## 3. Tìm kiếm sản phẩm

1. Nhập tên sản phẩm hoặc SKU vào ô tìm kiếm.
2. Kiểm tra danh sách sản phẩm phù hợp.
3. Chọn đúng sản phẩm cần bán.
4. Xác nhận sản phẩm đã được thêm vào giỏ hàng.

## 4. Quét mã vạch

1. Đặt con trỏ vào ô nhập barcode hoặc ô tìm kiếm.
2. Quét mã vạch sản phẩm.
3. Kiểm tra SKU tương ứng được hiển thị.
4. Xác nhận đúng sản phẩm trước khi thêm vào giỏ hàng.

## 5. Quản lý giỏ hàng

1. Kiểm tra các sản phẩm trong giỏ hàng.
2. Kiểm tra số lượng và đơn giá.
3. Xác nhận tổng tiền đơn hàng.
4. Điều chỉnh thông tin cần thiết trước khi thanh toán.

## 6. Hoàn tất thanh toán CASH

1. Chọn Checkout hoặc nhấn F9.
2. Kiểm tra hộp thoại thanh toán.
3. Chọn phương thức CASH.
4. Nhập số tiền khách hàng đưa.
5. Xác nhận thanh toán.
6. Kiểm tra hóa đơn hoàn tất và tiền thừa.

## 7. Thanh toán QR

1. Mở hộp thoại thanh toán.
2. Chọn phương thức QR.
3. Làm theo hướng dẫn hiển thị trên POS.
4. Chỉ xác nhận thanh toán theo quy trình thu ngân xác nhận của ứng dụng.

**Lưu ý:** Quy trình QR hiện được kiểm thử là xác nhận mô phỏng bởi thu ngân. Hệ thống chưa tự động xác minh giao dịch chuyển khoản ngân hàng.

## 8. Phím tắt

| Phím tắt | Chức năng |
|---|---|
| F2 | Đưa con trỏ đến ô tìm kiếm sản phẩm |
| F9 | Mở màn hình thanh toán |
| Escape | Hủy hoặc đóng hộp thoại hiện tại |

## 9. Thông báo realtime

POS có thể hiển thị thông báo đơn hàng theo thời gian thực khi kết nối với backend. Khi có sự kiện mới, hãy kiểm tra nội dung thông báo và thông tin đơn hàng.

## 10. Các bước cần kiểm tra

Trước khi hoàn tất giao dịch:

- Xác nhận đúng sản phẩm và SKU.
- Kiểm tra số lượng và đơn giá.
- Kiểm tra tổng tiền.
- Xác nhận phương thức thanh toán và số tiền khách đưa.
- Kiểm tra hóa đơn sau khi thanh toán.

Nếu POS không thể kết nối backend hoặc không tải được thông tin sản phẩm, hãy báo lỗi cho quản trị viên hệ thống.

## 11. Phạm vi và giới hạn

Tài liệu mô tả các quy trình POS đã được kiểm thử trong môi trường test riêng của M7. Tài liệu không khẳng định các quy trình marketplace chưa được kiểm thử hoặc chức năng tự động xác minh thanh toán ngân hàng đã hoạt động.
