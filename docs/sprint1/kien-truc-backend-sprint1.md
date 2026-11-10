# Kiến trúc backend OISM – nền tảng Sprint 1

Theo `docs/ke-hoach/ke-hoach-du-an-oism.md` (mục 4, quyết định kiến trúc 1). Code cũ (CRUD Long-id, SSE) dùng thời kỳ đầu đã được gỡ khỏi repo; backend hiện tại nằm ở thư mục `backend/`. Cập nhật 11/11/2026 cho khớp code Sprint 5.

## 4 tầng (Clean Architecture)

| Tầng | Package `com.oism.…` | Nội dung | Được phụ thuộc |
|---|---|---|---|
| domain | `domain.model`, `domain.repository`, `domain.service`, `domain.exception`, `domain.event` | Entity JPA, interface repository, quy tắc thuần (`OrderStateMachine`, `CostCalculator`, `Ean13`), `ErrorCode`, `BusinessException`, sự kiện | không phụ thuộc tầng khác |
| application | `application.<module>` | Use case (`AuthService`, `OrderService`…), command/result, record `...View`, port (`AccessTokenIssuer`) | domain |
| infrastructure | `infrastructure.security / tenant / websocket / scheduler / persistence / seed / config` | JWT, `SecurityConfig`, xác thực webhook, bộ lọc tenant, STOMP, tác vụ nền, truy vấn tổng hợp báo cáo, dữ liệu mẫu, OpenAPI | domain, application |
| web | `web.<module>`, `web.error` | Controller, DTO, `GlobalExceptionHandler` | application (không gọi repository) |

Quy tắc: controller chỉ gọi service; domain không import web/infrastructure; module mới thêm package `application.<module>` và `web.<module>` song song. ArchUnit (`ArchitectureTest`) kiểm tra tự động trong CI.

**Vì sao không phải `controller/service/repository/model`?** Đây vẫn là Spring MVC (REST, `@RestController`), nhưng mã được chia theo **lớp kiến trúc** rồi mới theo **module nghiệp vụ**, để quy tắc phụ thuộc (domain không biết web/hạ tầng) được ArchUnit kiểm tra được. Tầng "View" của MVC là hai ứng dụng React (`frontend-admin`, `frontend-pos`).

## Đa tenant (NFR-TENANT-01)
- Mọi bảng nghiệp vụ có `tenant_id`; entity kế thừa `TenantScopedEntity` + `@Filter("tenantFilter")`.
- `JwtAuthenticationFilter` đặt `TenantContext` từ claim `tenantId`; webhook sàn đặt tenant từ API key (`WebhookApiKeyFilter`); `TenantFilterAspect` bật filter trước mỗi lần gọi repository.
- `@Filter` **không áp dụng cho `findById`** → repository bắt buộc có hàm `findByIdAndTenantId`. Không dùng `findById` cho dữ liệu của tenant.

## Lỗi thống nhất
JSON `{errorCode, message, details}`; `errorCode = <PREFIX><HTTP><NN>` định nghĩa trong `ErrorCode`. Cần mã mới: thêm vào enum **và** bảng mã lỗi trong kế hoạch. 401/403 từ Spring Security cũng trả đúng định dạng này (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`; webhook sai key → `OD40101`). Lỗi hệ thống không lộ stack trace.

## Xác thực
- Access token JWT HS256, 60 phút, claims `sub, tenantId, role, email`.
- Refresh token ngẫu nhiên 7 ngày, lưu SHA-256, xoay vòng mỗi lần refresh; dùng lại token đã thu hồi → thu hồi toàn bộ token của user.
- Phân quyền: `@PreAuthorize` ở controller (đã bật `@EnableMethodSecurity`).

## Chạy local
1. `docker compose up -d postgres` (DB `oism_db`).
2. `cd backend && ./mvnw spring-boot:run` (profile `dev`, Flyway tự chạy các migration `V1`…`V12`).
3. Swagger UI: http://localhost:8080/swagger-ui.html
4. Biến môi trường production: `OISM_JWT_SECRET` (≥ 32 byte), `OISM_DB_URL/USER/PASSWORD`, `OISM_CORS_ORIGINS`.

Chi tiết: `docs/huong-dan/huong-dan-chay-du-an.md`.

## Trạng thái kiểm chứng (11/11/2026)
`./mvnw verify` xanh: 115 unit test + 59 test tích hợp (Testcontainers PostgreSQL), ArchUnit 6 luật, cổng coverage ≥ 80% cho sổ cái, giữ hàng, giá vốn. Kết quả chi tiết: `docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md`.
