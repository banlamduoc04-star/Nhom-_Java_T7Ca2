# TV7 – Realtime, QA & Deployment

**Base source:** latest supplied `Nhom-_Java_T7Ca2-develop.zip`. The integration retains the latest develop files for all non-TV7 modules and changes only the TV7-owned paths listed below.

**TV7-owned changed paths:** `backend/src/main/java/com/oism/infrastructure/websocket/WebSocketConfig.java`, `backend/src/test/java/com/oism/infrastructure/websocket/RealtimeEventListenerConcurrencyTest.java`, `frontend-pos/src/SaleScreen.tsx`, `frontend-pos/src/api.ts`, `frontend-pos/src/realtime.ts`, the POS build argument in `docker-compose.yml`, and this document.

## Phạm vi

Tài liệu này ghi nhận các phần TV7 phụ trách theo bảng phân công: realtime thông báo đơn hàng mới, kiểm thử concurrency 50 luồng, POS PWA, Docker Compose cho DB + Backend + Frontend và tổng kết triển khai. Các module nghiệp vụ của TV1–TV6 không thuộc phạm vi thay đổi của TV7.

## 1. Realtime thông báo đơn hàng mới

- Backend sử dụng WebSocket STOMP tại endpoint `/ws`; không sử dụng lại SSE cũ.
- `PosCheckoutService` phát `OrderCreatedEvent` trong luồng checkout.
- `RealtimeEventListener` phát event sau khi transaction commit tới `/topic/tenants/{tenantId}/orders/new`.
- POS lấy `tenantId` từ API `/me`, kết nối STOMP bằng JWT và chỉ subscribe topic tenant tương ứng.
- `WebSocketConfig` xác thực JWT ở STOMP CONNECT và từ chối SUBSCRIBE không thuộc tenant trong JWT để ngăn subscribe chéo tenant.

## 2. QA concurrency

Test `RealtimeEventListenerConcurrencyTest` khởi chạy 50 worker thread, đồng bộ thời điểm bắt đầu và xác minh listener gửi đủ 50 sự kiện tới topic tenant.

Chạy test:

```powershell
cd backend
.\mvnw.cmd -Dtest=RealtimeEventListenerConcurrencyTest test
```

Đây là kiểm thử concurrency ở tầng phát sự kiện realtime; không thay thế kiểm thử tải cơ sở dữ liệu hoặc kiểm thử khóa tồn kho end-to-end.

## 3. POS PWA

- `public/manifest.webmanifest`, `public/sw.js`, `public/icon.svg` và `public/favicon.svg` cung cấp metadata, service worker và icon.
- `src/main.tsx` đăng ký service worker khi ứng dụng được tải ngoài Vite localhost dev server.
- `src/realtime.ts` dùng WebSocket STOMP native, tự kết nối lại sau 5 giây và đăng ký nhận thông báo đơn hàng mới.
- Dữ liệu giả (`VITE_USE_MOCK=true`) không mở kết nối realtime.

## 4. Docker Compose

Chạy toàn hệ thống:

```powershell
docker compose up -d --build
```

Chạy cả giao diện Admin và POS:

```powershell
docker compose --profile frontend up -d --build
```

POS build-time API URL phải dùng `VITE_API_URL` vì frontend POS được xây dựng bằng Vite. Admin vẫn giữ `NEXT_PUBLIC_API_URL` theo cấu hình frontend-admin.

## 5. Kiểm tra trước PR

```powershell
# Backend unit test mới
cd backend
.\mvnw.cmd -Dtest=RealtimeEventListenerConcurrencyTest test

# Frontend POS
cd ..\frontend-pos
npm ci
npm run build
npm test

# Quay lại thư mục gốc để kiểm tra Compose
cd ..
docker compose config
```

## Giới hạn xác minh

Các lệnh kiểm thử cần chạy trong môi trường có Maven Wrapper dependencies, npm registry/cache và Docker Engine. Việc tạo tài liệu không được xem là bằng chứng các lệnh trên đã chạy thành công.


## 6. Kết quả xác minh trong môi trường đóng gói

- Đã kiểm tra cấu trúc YAML của `docker-compose.yml`: parse thành công.
- Đã đối chiếu source POS với hợp đồng `/me`: backend trả `tenantId`; POS type `Me` đã khai báo trường này.
- Đã kiểm tra luồng source: checkout hiện tại của team phát `OrderCreatedEvent`; listener phát tới `/topic/tenants/{tenantId}/orders/new`; POS đăng ký đúng topic tenant sau khi CONNECT bằng JWT.
- Chưa xác nhận được Maven test, frontend build/test hoặc Docker runtime trong môi trường đóng gói này: Maven Wrapper không tải được Maven distribution, npm offline thiếu gói `why-is-node-running`, và Docker Engine không được cài đặt. Không xem các hạng mục này là đã PASS.

Các lệnh nghiệm thu cần chạy trên máy có đủ công cụ và mạng:

```powershell
cd backend
.\mvnw.cmd -Dtest=RealtimeEventListenerConcurrencyTest test

cd ..\frontend-pos
npm ci
npm run build
npm test

cd ..
docker compose config
docker compose --profile frontend build frontend-pos backend
docker compose --profile frontend up -d
docker compose ps
```
