# M7 - Báo cáo kiểm thử E2E và QA POS

## 1. Môi trường kiểm thử

- Nhánh Git: `feature/m7-pos-realtime-pr`
- Địa chỉ POS: `http://localhost:3002`
- Địa chỉ API: `http://localhost:8081/api/v1`
- Cơ sở dữ liệu: Database kiểm thử riêng của M7
- Trình duyệt: Playwright Chromium
- Công cụ kiểm thử: Playwright
- Ngày kiểm thử: 2026-11-08

## 2. Kết quả kiểm thử đã xác minh

| Hạng mục kiểm thử | Kết quả | Bằng chứng |
|---|---|---|
| Đăng nhập thu ngân | PASS | Kiểm thử đăng nhập bằng Playwright thành công |
| Tìm kiếm SKU | PASS | Tìm `JEAN-L` và xác minh sản phẩm xuất hiện trong giỏ hàng |
| Tra cứu mã vạch | PASS | Mã vạch `8930000000132` trả về sản phẩm `JEAN-L` |
| Quy trình thanh toán CASH | PASS - xác minh thủ công | Admin xác nhận đơn `POS-261108-8235`, JEAN-L x1, tổng tiền 200.000 VND |
| Lịch sử trạng thái đơn hàng | PASS - xác minh thủ công | Đơn hàng chuyển từ Nháp -> Giữ hàng -> Đã xác nhận -> Hoàn tất; lý do cuối: Thanh toán POS thành công |
| Thông báo đơn hàng Realtime | PASS | POS hiển thị thông báo cho đơn `POS-261108-8059` |
| Kiểm thử đồng thời Backend Realtime | PASS | 1 bài kiểm thử thành công |
| Kiểm thử Backend POS Checkout | PASS | 3 bài kiểm thử thành công |
| Nhập hàng | PASS - xác minh thủ công | Phiếu `PN-20261108-5936` được xác nhận; tồn kho JEAN-XXL tăng từ 20 lên 21 |
| Bán hàng POS sau khi nhập kho | PASS - xác minh thủ công | Đơn `POS-261108-1341`; bán 1 sản phẩm JEAN-XXL |
| Đối soát sổ kho | PASS - xác minh thủ công | Sổ kho ghi nhận nhập hàng +1 và đơn POS -1; tồn kho cuối cùng là 20 |

### Ghi chú về lỗi kiểm tra Checkout bằng Playwright

Lần chạy Playwright gần nhất đã tạo đơn `POS-261108-8235`. Sau đó, đơn hàng được kiểm tra và xác minh thành công trên Admin.

Lỗi kiểm thử ban đầu xuất phát từ bộ định vị Dialog không sử dụng `exact: true`, dẫn đến việc khớp cả hộp thoại thanh toán và hộp thoại hóa đơn.

Bộ định vị đã được cập nhật bằng `exact: true`. Tuy nhiên, bài kiểm thử sau khi cập nhật chưa được chạy lại, vì vậy chưa đánh dấu kiểm thử Checkout tự động là PASS.

Kết quả lần chạy Playwright gần nhất:
- 6 bài kiểm thử PASS.
- 2 bài kiểm thử FAIL.
- Một lỗi còn lại liên quan đến tình trạng tràn chiều ngang trên giao diện Mobile.

## 3. Đo lường hiệu năng API

Phương pháp đo:
- Gửi 5 yêu cầu khởi động trước khi đo.
- Đo 100 yêu cầu cho mỗi API.
- Các yêu cầu được thực hiện tuần tự bằng Playwright APIRequestContext.
- Thời gian đo bao gồm thời gian gửi yêu cầu, nhận phản hồi và đọc nội dung phản hồi ở phía Client.

Mục tiêu: p95 < 200 ms.

Kết quả đo mới nhất:

| Chỉ số | Tìm kiếm SKU | Tra cứu mã vạch |
|---|---:|---:|
| Endpoint | `GET /api/v1/skus` | `GET /api/v1/skus/lookup` |
| Số yêu cầu khởi động | 5 | 5 |
| Số yêu cầu đo | 100 | 100 |
| Số yêu cầu thành công | 100 | 100 |
| Số lỗi | 0 | 0 |
| p50 | 21.26 ms | 10.24 ms |
| p95 | 35.12 ms | 14.98 ms |
| p99 | 58.97 ms | 20.66 ms |
| Trung bình | 23.00 ms | 10.90 ms |
| Mục tiêu p95 | < 200 ms | < 200 ms |
| Kết quả | PASS | PASS |

Cả hai API đều đáp ứng mục tiêu p95 dưới 200 ms.

Lưu ý: Đây là kết quả đo tuần tự trong môi trường kiểm thử riêng của M7. Kết quả không đại diện cho hiệu năng khi hệ thống chạy trong môi trường Production hoặc có nhiều người dùng truy cập đồng thời.

## 4. Kiểm thử giao diện Responsive

| Thiết bị | Kích thước màn hình | Kết quả |
|---|---|---|
| Desktop | 1366 x 768 | PASS |
| Tablet | 768 x 1024 | PASS |
| Mobile | 390 x 844 | FAIL |

Kết quả kiểm tra trên Mobile phát hiện tình trạng tràn chiều ngang:
- Chiều rộng thực tế của Document: 396 px.
- Chiều rộng Viewport: 390 px.
- Phần bị tràn: 6 px.

Quy tắc CSS `.login` được xác định nằm trong file `frontend-pos/src/index.css`. File này chưa được xác nhận thuộc phạm vi chỉnh sửa của M7.

M7 chưa tự ý chỉnh sửa file trên. Cần xác nhận quyền sở hữu trước khi đề xuất sửa lỗi.

Các bài kiểm thử hiện tại chỉ kiểm tra trang đăng nhập ban đầu. Việc kiểm thử Responsive trên giao diện POS sau khi đăng nhập vẫn chưa hoàn thành.

## 5. Kiểm tra Marketplace theo phương pháp Black-box

Trang Đơn hàng trên Admin được kiểm tra bằng môi trường thử nghiệm riêng của M7.

| Kênh bán hàng | Kết quả | Bằng chứng |
|---|---|---|
| Shopee | Chưa có dữ liệu | Bộ lọc trả về danh sách rỗng |
| TikTok | Chưa có dữ liệu | Bộ lọc trả về danh sách rỗng, không xuất hiện lỗi API rõ ràng |
| Lazada | BLOCKED | API trả về HTTP 400 khi lọc `channel=LAZADA` |

### Phân tích lỗi Lazada

- `OrderController.java` nhận `OrderChannel` làm tham số lọc kênh bán hàng.
- `OrderChannel.java` hiện khai báo các giá trị `POS`, `MANUAL`, `SHOPEE` và `TIKTOK`.
- Giá trị `LAZADA` chưa tồn tại trong enum.
- Lịch sử Git xác định DungZL là người sở hữu thay đổi liên quan đến enum này.

M7 không chỉnh sửa bất kỳ file mã nguồn nào thuộc Admin hoặc Backend.

Lỗi bộ lọc Lazada cần được báo lại cho thành viên phụ trách Backend.

Hiện tại chưa xác minh được các quy trình tạo đơn Marketplace, đồng bộ Webhook và xử lý đơn hàng đầu cuối.

Việc danh sách đơn hàng của một kênh đang trống không chứng minh rằng tích hợp Marketplace hoạt động chính xác.

## 6. Tài liệu bàn giao

| Tài liệu | Kết quả | Đường dẫn |
|---|---|---|
| Hướng dẫn sử dụng POS | CREATED | `docs/m7/TV7-pos-user-guide.md` |
| Báo cáo kiểm thử E2E và QA | UPDATED | `docs/m7/TV7-e2e-qa-report.md` |

## 7. Các hạn chế và hạng mục cần kiểm thử thêm

- Chưa xác minh quy trình tạo sản phẩm bằng E2E.
- Chưa xác minh quy trình xử lý đơn hàng Marketplace bằng E2E.
- Chưa kiểm thử Responsive đầy đủ trên giao diện POS sau khi đăng nhập.
- Lỗi tràn chiều ngang trên Mobile cần được xác nhận quyền sở hữu và kiểm tra lại sau khi xử lý.
- Bài kiểm thử Checkout Playwright sau khi sửa bộ định vị cần được chạy lại.
- Thanh toán QR hiện hoạt động theo cơ chế thu ngân xác nhận mô phỏng. Hệ thống chưa tích hợp xác minh giao dịch ngân hàng tự động.
- Kết quả hiệu năng hiện chỉ dựa trên các yêu cầu API tuần tự trong môi trường kiểm thử riêng.

## 8. Tổng kết trạng thái QA

### Các hạng mục đã xác minh

- Đăng nhập thu ngân, tìm kiếm SKU và tra cứu mã vạch.
- Quy trình thanh toán CASH và hiển thị hóa đơn, đã xác minh thủ công qua Admin.
- Đơn hàng đã chuyển đến trạng thái Hoàn tất.
- Thông báo đơn hàng Realtime.
- Nhập hàng, bán hàng POS và đối soát sổ kho.
- Hai API tìm kiếm SKU và tra cứu mã vạch đáp ứng mục tiêu p95.
- Kiểm tra Responsive trang đăng nhập trên Desktop và Tablet.
- Đã tạo tài liệu hướng dẫn sử dụng POS.

### Các hạng mục còn tồn đọng hoặc bị chặn

- Kiểm thử quy trình tạo sản phẩm.
- Kiểm thử E2E đơn hàng Marketplace.
- Lỗi bộ lọc kênh Lazada (ngoài phạm vi sở hữu của M7).
- Xác nhận quyền sở hữu và kiểm tra lại lỗi tràn giao diện Mobile.
- Kiểm thử Responsive đầy đủ sau khi đăng nhập POS.
- Chạy lại bài kiểm thử Checkout Playwright sau khi sửa bộ định vị.

Báo cáo chỉ ghi nhận những kết quả đã được xác minh. Các hạng mục đang chờ kiểm thử hoặc bị chặn không được đánh dấu là hoàn thành.
