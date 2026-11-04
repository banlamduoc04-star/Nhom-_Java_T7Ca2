# Kiến trúc backend OISM – nền tảng Sprint 1

Theo `docx/Kế hoạch dự án OISM – 5 Sprint, 7 thành viên.md`. Code cũ (CRUD Long-id, SSE) được cách ly tại `omnichannel-oism/backend/legacy-reference/` để các sprint sau tham khảo, không nằm trong build.

## 4 tầng (Clean Architecture)

| Tầng | Package `com.oism.…` | Nội dung | Được phụ thuộc |
|---|---|---|---|
| domain | `domain.model`, `domain.repository`, `domain.exception` | Entity JPA, repository interface, `ErrorCode`, `BusinessException` | không phụ thuộc tầng khác |
| application | `application.<module>` | Use case (`AuthService`), command/result, port (`AccessTokenIssuer`) | domain |
| infrastructure | `infrastructure.security / tenant / config` | JWT, SecurityConfig, tenant filter, OpenAPI | domain, application |
| web | `web.<module>`, `web.error` | Controller, DTO, `GlobalExceptionHandler` | application (không gọi repository) |

Quy tắc review: controller chỉ gọi service; domain không import web/infrastructure; module mới thêm package `application.<module>` và `web.<module>` song song với `auth`.

## Đa tenant (NFR-TENANT-01)
- Mọi bảng nghiệp vụ có `tenant_id`; entity kế thừa `TenantScopedEntity` + `@Filter("tenantFilter")`.
- `JwtAuthenticationFilter` đặt `TenantContext` từ claim `tenantId`; `TenantFilterAspect` bật filter trước mỗi lần gọi repository.
- `@Filter` **không áp dụng cho `findById`** → repository bắt buộc có hàm `findByIdAndTenantId` (xem `AppUserRepository`). Không dùng `findById` cho dữ liệu của tenant.

## Lỗi thống nhất
JSON `{errorCode, message, details}`; `errorCode = <PREFIX><HTTP><NN>` định nghĩa trong `ErrorCode`. Cần mã mới: thêm vào enum **và** bảng mã lỗi trong kế hoạch. 401/403 từ Spring Security cũng trả đúng định dạng này (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`). Lỗi hệ thống không lộ stack trace.

## Xác thực
- Access token JWT HS256, 60 phút, claims `sub, tenantId, role, email`.
- Refresh token ngẫu nhiên 7 ngày, lưu SHA-256, xoay vòng mỗi lần refresh; dùng lại token đã thu hồi → thu hồi toàn bộ token của user.
- Phân quyền: `@PreAuthorize("hasRole('OWNER')")` (đã bật `@EnableMethodSecurity`).

## Chạy local
1. `docker compose up -d` (Postgres, DB `oism_db`).
2. `cd omnichannel-oism/backend && mvn spring-boot:run` (profile `dev`, Flyway tự migrate `V1__auth_foundation.sql`).
3. Swagger UI: http://localhost:8080/swagger-ui.html
4. Biến môi trường production: `OISM_JWT_SECRET` (≥ 32 byte), `OISM_DB_URL/USER/PASSWORD`, `OISM_CORS_ORIGINS`.

## Trạng thái kiểm chứng
Code Sprint 1 **chưa được biên dịch/chạy test** (môi trường tạo code không tải được Maven). Việc cần làm đầu tiên: `mvn -q verify`.
