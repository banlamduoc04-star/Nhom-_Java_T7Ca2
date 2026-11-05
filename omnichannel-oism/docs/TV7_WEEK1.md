BÁO CÁO CÔNG VIỆC TUẦN 1 – THÀNH VIÊN TV7

1. TỔNG QUAN CÔNG VIỆC

Trong tuần 1, thành viên TV7 thực hiện các công việc liên quan đến Backend, kiểm thử, Frontend PWA, triển khai Docker và tài liệu dự án OISM.

Các nội dung đã thực hiện gồm:

- Xây dựng chức năng thông báo đơn hàng theo thời gian thực bằng SSE.
- Viết kiểm thử xử lý nhiều luồng đồng thời.
- Cấu hình Progressive Web App (PWA) cho Frontend POS.
- Xây dựng Dockerfile và Docker Compose cho hệ thống.
- Kiểm tra hoạt động của các dịch vụ sau khi triển khai.

2. BACKEND – THÔNG BÁO ĐƠN HÀNG THEO THỜI GIAN THỰC

2.1. Công nghệ sử dụng

- Java 21.
- Spring Boot.
- Spring Web MVC.
- Server-Sent Events (SSE).

2.2. Các thành phần đã xây dựng

Các file được triển khai trong package com.oism.realtime:

- OrderEvent.java: Định nghĩa dữ liệu sự kiện đơn hàng, gồm mã đơn hàng, loại sự kiện và thời gian xảy ra.
- OrderEventService.java: Quản lý các kết nối SSE, lưu danh sách client đang kết nối và gửi sự kiện đến các client.
- OrderEventController.java: Cung cấp API để client đăng ký nhận sự kiện đơn hàng.
- LocalOrderEventTestController.java: Cung cấp API kiểm thử gửi sự kiện trong môi trường local.

2.3. Các API đã xây dựng

API đăng ký nhận sự kiện:

GET /api/v1/events/orders

API trả về dữ liệu dạng text/event-stream, cho phép Frontend duy trì kết nối và nhận thông báo khi có sự kiện mới.

API kiểm thử:

GET /api/v1/events/test/orders/{orderId}

API được sử dụng trong môi trường local để giả lập sự kiện đơn hàng mới.

2.4. Kết quả kiểm thử realtime

Đã khởi động Backend và Frontend, sau đó gọi API kiểm thử với mã đơn hàng DOCKER-TV7-001.

Kết quả:

- Backend phản hồi thành công.
- Kết nối SSE hoạt động bình thường.
- Frontend POS nhận được thông báo realtime.

Lưu ý: Chức năng hiện được kiểm thử bằng API giả lập trong profile local. Chưa tích hợp việc phát sự kiện trực tiếp vào quy trình tạo đơn hàng thực tế của hệ thống.

3. BACKEND – KIỂM THỬ XỬ LÝ ĐỒNG THỜI

Đã xây dựng file OrderEventServiceConcurrencyTest.java để kiểm tra khả năng xử lý nhiều luồng đồng thời của OrderEventService.

Nội dung kiểm thử:

- Tạo 50 luồng đăng ký kết nối SSE đồng thời.
- Thực hiện 50 luồng gửi sự kiện đồng thời.
- Kiểm tra số lượng kết nối và số sự kiện được gửi đến các client.

Kết quả mong đợi:

- Có đủ 50 kết nối SSE.
- Mỗi client nhận được 1 sự kiện kết nối và 50 sự kiện đơn hàng.
- Không xảy ra lỗi khi nhiều luồng cùng thao tác trên danh sách client.

Lệnh chạy kiểm thử:

.\backend\mvnw.cmd -f .\backend\pom.xml "-Dtest=OrderEventServiceConcurrencyTest" test

Kết quả: BUILD SUCCESS.

Lưu ý: Đây là kiểm thử đồng thời ở tầng Service, không phải kiểm thử tải HTTP thực tế.

4. FRONTEND – PROGRESSIVE WEB APP (PWA)

PWA được cấu hình trong thư mục frontend-pos.

4.1. Web App Manifest

File thực hiện: frontend-pos/app/manifest.ts

Các thông tin đã cấu hình:

- Tên ứng dụng: OISM POS.
- Tên hiển thị ngắn: OISM POS.
- Chế độ hiển thị: standalone.
- Đường dẫn khởi động: /.
- Màu giao diện và màu nền.
- Biểu tượng ứng dụng kích thước 192x192 và 512x512.

4.2. Service Worker

File thực hiện: frontend-pos/public/sw.js

Các chức năng chính:

- Tạo bộ nhớ đệm cho một số trang của ứng dụng.
- Lưu bộ nhớ đệm cho tài nguyên tĩnh của Next.js.
- Hỗ trợ hiển thị trang đã lưu khi mất kết nối mạng trong một số trường hợp.
- Xóa bộ nhớ đệm phiên bản cũ khi kích hoạt phiên bản mới.
- Không can thiệp vào các yêu cầu API và kết nối SSE.

4.3. Đăng ký Service Worker

File thực hiện: frontend-pos/app/ServiceWorkerRegistration.tsx

Sử dụng React useEffect để đăng ký Service Worker khi ứng dụng được tải.

4.4. Kết quả kiểm tra PWA

Đã kiểm tra các nội dung sau:

- Manifest được tải thành công.
- Hai biểu tượng PNG hiển thị bình thường.
- Service Worker được đăng ký và có trạng thái activated and running.
- Trình duyệt Chrome hiển thị tùy chọn cài đặt ứng dụng.
- Đã cài đặt thử ứng dụng OISM POS thành công.

5. TRIỂN KHAI HỆ THỐNG BẰNG DOCKER

5.1. Các thành phần triển khai

Đã xây dựng Dockerfile cho Backend, Frontend POS và Frontend Admin.

File cấu hình tổng thể: docker-compose.yml.

Hệ thống gồm 4 dịch vụ:

- PostgreSQL: Sử dụng PostgreSQL 16, cổng 5432.
- Backend: Sử dụng Spring Boot, cổng 8080.
- Frontend POS: Sử dụng Next.js, cổng 3000.
- Frontend Admin: Sử dụng Next.js, cổng 3001.

5.2. Các lệnh triển khai

Build các Docker image:

docker compose build

Khởi động hệ thống:

docker compose up -d

Kiểm tra trạng thái các dịch vụ:

docker compose ps

Xem log Backend:

docker compose logs --tail=80 backend

Dừng các dịch vụ:

docker compose down

Lưu ý: Không thêm tùy chọn -v khi dừng nếu muốn giữ dữ liệu trong volume PostgreSQL.

5.3. Kết quả triển khai

Đã build thành công 3 Docker image:

- Backend.
- Frontend POS.
- Frontend Admin.

Sau khi chạy Docker Compose, cả 4 container đều hoạt động:

- PostgreSQL: Up (healthy).
- Backend: Up.
- Frontend POS: Up.
- Frontend Admin: Up.

Đã truy cập và kiểm tra giao diện:

- Frontend POS: http://localhost:3000
- Frontend Admin: http://localhost:3001

Cả hai giao diện đều mở được bình thường.

Đã kiểm tra thêm API SSE trên Backend chạy trong Docker và xác nhận Frontend POS nhận được thông báo kiểm thử.

5.4. Lưu ý về bảo mật

Các mật khẩu và JWT secret trong cấu hình hiện tại chỉ phục vụ môi trường phát triển local. Khi triển khai thực tế cần thay bằng thông tin bảo mật riêng và quản lý bằng biến môi trường hoặc hệ thống quản lý secrets. Không sử dụng các giá trị mẫu này trong môi trường production.

6. TỔNG KẾT KẾT QUẢ

- Xây dựng API SSE: Hoàn thành phần demo.
- Kiểm thử 50 luồng đồng thời: BUILD SUCCESS.
- Cấu hình PWA: Hoàn thành.
- Đăng ký Service Worker: Hoạt động.
- Cài đặt thử PWA trên Chrome: Thành công.
- Xây dựng Dockerfile và Docker Compose: Build thành công.
- Khởi động 4 dịch vụ: Thành công.
- Kiểm tra Frontend POS và Frontend Admin: Hoạt động.
- Kiểm tra thông báo SSE qua Docker: Thành công.
- Tích hợp SSE với quy trình tạo đơn hàng thật: Chưa thực hiện.

7. KẾT LUẬN

Trong tuần 1, thành viên TV7 đã triển khai được các phần công việc chính gồm thông báo realtime bằng SSE, kiểm thử đồng thời, cấu hình PWA và triển khai hệ thống bằng Docker Compose.

Các thành phần đã được kiểm tra trên môi trường local và Docker. Kết quả cho thấy Backend, Frontend POS, Frontend Admin và PostgreSQL có thể khởi động và hoạt động cùng nhau.

Trong giai đoạn tiếp theo, cần phối hợp với thành viên phụ trách nghiệp vụ đơn hàng để tích hợp sự kiện SSE vào quy trình tạo đơn hàng thực tế, đồng thời tiếp tục hoàn thiện tài liệu và kiểm thử toàn hệ thống.