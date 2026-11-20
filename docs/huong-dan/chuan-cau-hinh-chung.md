# Chuẩn cấu hình và mã nguồn chung

Tài liệu bắt buộc cho cả nhóm. Khi viết hoặc duyệt code, **đối chiếu với file này**; nếu code mới khác chuẩn, sửa code (hoặc đề xuất sửa chuẩn qua pull request riêng). Ví dụ trong tài liệu lấy từ mã nguồn thật để dùng làm mẫu.

## 1. Nguyên tắc

1. Kế hoạch dự án OISM là nguồn sự thật cho yêu cầu (FR/NFR), bảng dữ liệu, API và mã lỗi. Chuẩn này chỉ nói *cách viết code* để thực hiện kế hoạch.
2. Mỗi quy tắc nghiệp vụ nằm đúng **một** nơi (service hoặc domain), không lặp ở controller/frontend.
3. Mọi quy tắc quan trọng có test. Quy tắc nào ArchUnit kiểm tra được thì ArchUnit kiểm tra (CI đỏ nếu vi phạm).

## 2. Cấu trúc backend và quy tắc phụ thuộc

Gói gốc `com.oism`, 4 lớp (Clean Architecture). Mũi tên là "được phép phụ thuộc vào":

```text
web ──► application ──► domain ◄── infrastructure
```

| Lớp | Chứa | Không được |
|---|---|---|
| `domain.model` | Entity JPA, enum | Biết `web`, `application`, `infrastructure` |
| `domain.repository` | Interface Spring Data | Chứa logic nghiệp vụ |
| `domain.service` | Quy tắc nghiệp vụ thuần (không Spring): `CostCalculator`, `Ean13`, `OrderStateMachine` | Gọi DB, gọi Spring |
| `domain.exception` | `BusinessException`, `ErrorCode` | — |
| `application.<module>` | Service (`@Service`, `@Transactional`), command, **View** (record trả về) | Biết `web`, `infrastructure` |
| `web.<module>` | Controller, request DTO, xử lý lỗi | Gọi `domain.repository`; chứa logic nghiệp vụ |
| `infrastructure` | JWT, Security, tenant filter, OpenAPI, seed | — |

Các quy tắc trên được kiểm tra tự động trong `ArchitectureTest`. Mỗi module một gói con: `auth`, `branch`, `user`, `catalog` (PROD), `inventory` (INV/COST), `order` (ORD/RSE), thêm `report` ở Sprint 3–4.

**Mẫu một tính năng mới** (ví dụ `GET /widgets`):

1. `domain/model/Widget.java` (entity, kế thừa `TenantScopedEntity`, gắn `@Filter`).
2. `domain/repository/WidgetRepository.java`, mọi hàm truy vấn có `tenantId` (xem mục 7).
3. `application/widget/WidgetView.java` (record) + `WidgetService.java`.
4. `web/widget/WidgetController.java` với request DTO là record lồng bên trong.
5. Migration `V{n}__widget.sql`.
6. Test: unit cho quy tắc, `*IT` cho API (kể cả 403 và 404 chéo tenant).

## 3. Quy ước đặt tên và viết code

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Lớp | `PascalCase`, danh từ | `PurchaseReceiptService` |
| Service | `<Đối tượng>Service` | `CategoryService` |
| Trả về từ service | `<Đối tượng>View` (record) | `SkuView`, `ReceiptView` |
| Dữ liệu đầu vào service | tham số rời hoặc `record` đặt tên rõ | `ReceiptLine`, `LedgerEntry` |
| Request DTO | record **lồng trong controller**, hậu tố `Request` | `CategoryController.CategoryRequest` |
| Controller | `<Đối tượng>Controller`, `@RequestMapping("/api/v1/<số-nhiều-kebab>")` | `/api/v1/purchase-receipts` |
| Bảng | `snake_case` số ít (trừ `orders`, vì `order` là từ khóa SQL) | `purchase_receipt_item` |
| Cột | `snake_case` | `balance_after` |
| Enum giá trị | `UPPER_SNAKE_CASE`, lưu dạng chuỗi (`@Enumerated(STRING)`) | `TRANSFER_OUT` |
| Hằng số | `UPPER_SNAKE_CASE` | `REFRESH_TTL` |
| Test | `<Lớp>Test` (unit), `<Chủ đề>IT` (tích hợp, cần Docker) | `CostCalculatorTest`, `InventoryIT` |

Quy tắc code:

- Java 17: dùng `record`, `switch` biểu thức, `List.of`. Không dùng Lombok (dự án không cài).
- Inject qua **constructor**, không dùng `@Autowired` trên field (trừ test).
- Không dùng `float`/`double` cho tiền: dùng `BigDecimal`, làm tròn `HALF_UP`, 2 chữ số.
- Thời gian lưu và truyền bằng `Instant` (UTC); frontend đổi sang giờ địa phương khi hiển thị.
- Mã nguồn **không có** mật khẩu, khóa, token. Mọi bí mật đọc từ biến môi trường.
- Comment viết tiếng Việt, giải thích *vì sao* và gắn mã yêu cầu khi có (`FR-INV-02`); không comment điều code đã nói rõ.
- Thông báo cho người dùng (message của `BusinessException`, validation) viết **tiếng Việt có dấu**.
- Không `catch (Exception)` để nuốt lỗi. Lỗi nghiệp vụ ném `BusinessException(ErrorCode.XXX, "...")`; lỗi lạ để `GlobalExceptionHandler` đổi thành `SY50001`.

## 4. Quy ước API

### 4.1. Đường dẫn và phương thức

- Tiền tố `/api/v1`, JSON hai chiều, `kebab-case` số nhiều: `/purchase-receipts`, `/inventory-transactions`.
- Tạo mới → `POST`, trả **201** kèm đối tượng. Sửa toàn bộ → `PUT`. Đổi một thuộc tính → `PATCH .../status`. Xóa → `DELETE`, trả **204**. Hành động nghiệp vụ → `POST /{id}/<động-từ>` (`/confirm`, `/reserve`).
- Không có API sửa/xóa sổ cái tồn kho (NFR-SEC-03).

### 4.2. Phân trang

`?page=0&size=20` (mặc định 20, tối đa 100). Trả `PageResult`: `{content, page, size, totalElements, totalPages}`. Danh sách nhỏ (danh mục, thương hiệu, chi nhánh) trả mảng.

### 4.3. Lỗi

Mọi lỗi cùng một dạng:

```json
{ "errorCode": "IV40901", "message": "Không đủ hàng khả dụng (còn 2)", "details": [] }
```

Mã `<TIỀN TỐ><HTTP><SỐ>`: `AU` xác thực, `PR` sản phẩm, `IV` tồn kho, `OD` đơn hàng, `RP` báo cáo, `SY` hệ thống. Khai báo trong enum `ErrorCode` **và** cập nhật bảng mã lỗi trong kế hoạch. Lỗi validation trả 400 kèm `details: [{field, message}]`, tiền tố do `GlobalExceptionHandler` suy ra từ đường dẫn (xem `validationCode`; module mới phải bổ sung vào đây).

| Tình huống | HTTP | Ghi chú |
|---|---|---|
| Thiếu/sai/hết hạn token | 401 `AU40101` | |
| Sai vai trò | 403 `AU40301` | |
| Không thấy bản ghi, **hoặc thuộc tenant khác** | 404 | Không bao giờ trả 403 cho dữ liệu tenant khác |
| Trùng, sai trạng thái | 409 | |

### 4.4. Phân quyền

Dùng `@PreAuthorize` ở **controller** (cấp lớp hoặc phương thức):

| Vai trò | Biểu thức |
|---|---|
| Chỉ Owner | `hasRole('OWNER')` |
| Owner + Staff | `hasAnyRole('OWNER','STAFF')` |
| Mọi vai trò | không cần chú thích (đã yêu cầu đăng nhập) |

Bảng quyền theo từng endpoint nằm ở mục "Danh sách API" của kế hoạch. Endpoint công khai chỉ gồm `register`, `login`, `refresh`, Swagger, `actuator/health` (khai báo trong `SecurityConfig`).

### 4.5. Validation và Swagger

- Validation bằng Bean Validation trên request DTO, **message tiếng Việt**: `@NotBlank(message = "Tên danh mục không được để trống")`.
- Mỗi endpoint có `@Operation(summary = "...")`; controller có `@Tag`. Nêu rõ vai trò và mã lỗi chính trong `summary`.
- Tên sản phẩm theo luật của đề: `^[A-Z][a-zA-Z0-9\s]{2,50}$` (chữ hoa đầu, **không dấu**).

## 5. Cơ sở dữ liệu và Flyway

- Schema do Flyway quản lý (`ddl-auto: validate`). File `src/main/resources/db/migration/V{n}__{mô_tả}.sql`.
- **Không sửa migration đã merge.** Cần đổi thì thêm `V{n+1}`.
- Hiện đã có V1 AUTH, V2 PROD, V3 INV, V4 ORD. Hai người cùng tạo `V5` ở hai nhánh sẽ làm Flyway lỗi sau khi merge: **báo trong nhóm trước khi lấy số mới** và merge `develop` về nhánh mình trước khi đặt tên file.
- Mọi bảng nghiệp vụ có: `id UUID PRIMARY KEY`, `tenant_id UUID NOT NULL REFERENCES tenant(id)`, `created_at`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()` (trừ sổ cái chỉ có `created_at`).
- Mọi bảng chính (không phải bảng dòng chi tiết) có index `(tenant_id, created_at)`; bảng liên quan SKU có thêm `(tenant_id, sku_id)` (NFR-TENANT-02).
- Ràng buộc đặt bằng `CHECK`/`UNIQUE` ở DB **và** kiểm tra ở service để báo lỗi đẹp (DB là lớp bảo vệ cuối).
- Tên ràng buộc: `ck_<bảng>_<ý>`, `uq_<bảng>_<cột>`, `ix_<bảng>_<cột>`.
- Entity ánh xạ đúng cột; `@Enumerated(EnumType.STRING)`; JSON dùng `@JdbcTypeCode(SqlTypes.JSON)` (xem `Sku.attributes`).

## 6. Biến môi trường

Mọi cấu hình đổi theo môi trường đọc từ biến môi trường (mặc định dành cho dev). Không đặt giá trị thật trong `application*.yml`.

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `OISM_DB_URL` | `jdbc:postgresql://localhost:5432/oism_db` | JDBC URL |
| `OISM_DB_USER` / `OISM_DB_PASSWORD` | `postgres` / `postgres` | Tài khoản DB |
| `OISM_JWT_SECRET` | khóa dev trong `application-dev.yml` | Khóa ký JWT, **≥ 32 byte**, bắt buộc đặt riêng khi triển khai thật |
| `OISM_CORS_ORIGINS` | `http://localhost:5173` | Danh sách origin frontend, cách nhau dấu phẩy |
| `OISM_SEED_ENABLED` | `false` | `true` để nạp dữ liệu mẫu demo (chỉ dev/demo) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile Spring |
| `SERVER_PORT` | `8080` | Cổng backend |
| `POSTGRES_PASSWORD` | `postgres` | Mật khẩu DB cho `docker-compose.yml` |
| `VITE_API_URL` (frontend) | `http://localhost:8080/api/v1` | Địa chỉ API, nhúng lúc build |
| `VITE_USE_MOCK` (POS) | — | `true` để chạy bằng dữ liệu giả |

Thêm cấu hình mới: khai báo trong `@ConfigurationProperties` (như `JwtProperties`) hoặc `@Value` có mặc định, ghi vào bảng này **và** `docker-compose.yml` nếu cần.

Giá trị cố định theo kế hoạch: access token 60 phút, refresh token 7 ngày xoay vòng và lưu dạng băm SHA-256, mật khẩu BCrypt, tối đa 100 dòng/trang, giữ hàng mặc định 30 phút.

## 7. Quy tắc bắt buộc: multi-tenant và sổ cái

### 7.1. Cô lập tenant (NFR-TENANT-01)

1. `tenant_id` luôn lấy từ **JWT** (`AuthenticatedUser.tenantId()`), **không bao giờ** từ request body hay query.
2. Entity thuộc tenant kế thừa `TenantScopedEntity` và gắn `@Filter(name = TENANT_FILTER, condition = TENANT_FILTER_CONDITION)`.
3. Repository: **mọi** hàm truy vấn nhận `tenantId` (`findByIdAndTenantId`, `existsByTenantIdAndName...`). `@Filter` không áp dụng cho `findById`, nên **cấm dùng `findById` trực tiếp** cho dữ liệu tenant.
4. Tìm không thấy hoặc khác tenant → `404` (cùng mã `...40401`).
5. Mỗi API mới phải có test hai tenant (mẫu: `TenantIsolationIT`).

### 7.2. Sổ cái tồn kho (FR-INV-01)

1. Cột `on_hand` **chỉ** được sửa trong `InventoryLedgerService.post(...)`, cùng giao dịch với dòng `inventory_transaction` (Kế hoạch mục 4: "Không nơi nào khác được sửa `on_hand`"). Cột `reserved` (giữ hàng, không phải biến động kho nên không ghi sổ cái) chỉ được sửa trong `ReservationService`, sau khi khóa dòng bằng `findForUpdate`. Không viết `UPDATE stock_balance` ở nơi khác. Sau mỗi thay đổi `on_hand`/`reserved`, hai nơi này gọi `StockAlertService.checkStockLevel` trong cùng giao dịch (FR-REP-03).
2. `post` bắt buộc chạy trong giao dịch (`Propagation.MANDATORY`) và khóa dòng bằng `SELECT ... FOR UPDATE`. Khi khóa nhiều SKU, **khóa theo thứ tự `sku_id` tăng dần** để tránh deadlock.
3. Sổ cái chỉ ghi thêm: không có hàm sửa/xóa, DB có trigger chặn. Sửa sai bằng bút toán bù.
4. Giá vốn chỉ đổi khi xác nhận phiếu nhập, qua `CostCalculator`; chụp `cost_price` vào dòng đơn khi đơn sang Confirmed (Sprint 3).
5. Chuyển trạng thái đơn chỉ qua `OrderStateMachine`; mọi lần chuyển ghi `order_status_history`.

## 8. Git, commit và pull request

- Nhánh: `main` (bản nộp), `develop` (tích hợp), `feature/<module>-<mô-tả>`. Không push thẳng vào `main`/`develop`.
- Một nhánh = một đầu việc Jira nhỏ. Rebase/merge `develop` thường xuyên để tránh xung đột migration.
- Commit theo [Conventional Commits](https://www.conventionalcommits.org), tiếng Việt, ≤ 72 ký tự dòng đầu:

  ```text
  feat(prod): thêm API danh mục nhiều cấp
  fix(inv): sửa tính giá vốn khi tồn cũ bằng 0
  test(ord): thêm test OrderStateMachine
  docs: cập nhật hướng dẫn chạy
  chore(ci): thêm job frontend
  ```

  Tiền tố: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`. Phạm vi: `auth`, `prod`, `inv`, `cost`, `ord`, `rse`, `pos`, `rep`, `sim`, `admin`, `ci`.
- Mỗi PR dùng mẫu `.github/pull_request_template.md`, ghi mã Jira (`JC-xx`) và mã yêu cầu (`FR-...`).
- Merge khi: CI xanh + 1 người duyệt + đã chạy được bằng Docker. Người duyệt dùng checklist ở mục 11.
- **Không** commit: `.env`, `target/`, `node_modules/`, `dist/`, `docs/internal/` (nội bộ), file IDE.

## 9. Kiểm thử

| Loại | Công cụ | Đặt ở | Chạy |
|---|---|---|---|
| Unit (service, domain) | JUnit 5, Mockito | `src/test/.../<gói>` | `./mvnw test` |
| Kiến trúc | ArchUnit | `architecture/ArchitectureTest` | `./mvnw test` |
| Tích hợp API | `@SpringBootTest` + Testcontainers PostgreSQL + MockMvc | `integration/*IT` | `./mvnw verify` (cần Docker) |
| Đồng thời | `ExecutorService` (mẫu: `InventoryIT.concurrentOutNeverOversells`) | `integration/` | `./mvnw verify` |
| Coverage | JaCoCo, cổng ≥ 80% cho `InventoryLedgerService`, `ReservationService`, `CostCalculator` (CI đỏ khi dưới ngưỡng) | — | `./mvnw -Pcoverage verify` |
| Frontend unit | Vitest | `src/*.test.ts` | `npm test` |
| API (bản Docker) | Postman + newman | `postman/*.postman_collection.json` | `npx newman run <collection> -e postman/OISM_Local.postman_environment.json` |
| E2E (bản Docker) | Playwright, Admin + POS ở 3 kích thước | `e2e/tests/*.spec.ts` | `cd e2e && npm ci && npx playwright install chromium && npm test` |
| Hiệu năng | SQL sinh dữ liệu + `EXPLAIN ANALYZE` | `backend/scripts/` | Xem `docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md` |

Quy định:

- Mỗi FR có ít nhất 1 test đúng và 1 test lỗi; tên test mô tả hành vi (`registerRejectsDuplicateEmailWithAU40901`).
- Test tích hợp tự tạo tenant riêng (`newTenant()`), không dùng dữ liệu của test khác.
- Mục tiêu coverage ≥ 80% cho sổ cái, giữ hàng, giá vốn (NFR-MAINT-02).
- Test không phụ thuộc thứ tự chạy, không `Thread.sleep` để chờ.

## 10. Frontend (Admin và POS)

Công nghệ theo kế hoạch: **React + Vite + TypeScript**; Admin dùng Ant Design; POS là PWA (có `manifest.webmanifest`).

- Cấu trúc `src/`: `api/` (axios + hàm gọi API theo module), `auth/`, `components/` (dùng chung), `pages/`.
- Mọi lời gọi API đi qua `api/client.ts`: tự gắn `Authorization`, tự làm mới token khi 401 (một lần, gộp nhiều request), thất bại thì về `/login`.
- Hiển thị lỗi bằng `errorText(error)` để thấy `message` và `errorCode` từ backend; không tự bịa thông báo lỗi.
- Mọi màn có trạng thái **đang tải**, **rỗng** (`locale.emptyText`), **lỗi**; thao tác xóa/hủy có hộp xác nhận (`Popconfirm`).
- Ẩn menu theo vai trò (`AppLayout`) **và** chặn route (`RequireRole`); backend vẫn là chốt cuối.
- Kiểu dữ liệu API khai báo ở `api/types.ts`, khớp với record `...View` của backend; UUID là `string`, tiền là `number`.
- Không dùng `any`; `npm run build` (gồm `tsc -b`) phải sạch lỗi trước khi mở PR.
- Chữ giao diện tiếng Việt có dấu; định dạng tiền `toLocaleString('vi-VN')` + ` ₫`.
- Lưu ý bảo mật: token đang lưu `localStorage` (đủ cho đồ án); không ghi token vào log hay URL.

## 11. Checklist duyệt pull request

**Backend**
- [ ] Đúng lớp, `ArchitectureTest` xanh; controller không gọi repository.
- [ ] Mọi truy vấn có `tenantId`; có test hai tenant → 404 chéo.
- [ ] `@PreAuthorize` đúng bảng quyền; có test 401/403.
- [ ] Mã lỗi đúng bảng; validation có message tiếng Việt; Swagger có `@Operation`.
- [ ] Migration mới đúng số, không sửa migration cũ, có index `(tenant_id, created_at)`.
- [ ] Thay đổi tồn kho chỉ qua `InventoryLedgerService`; có `@Transactional`.
- [ ] Không có bí mật trong code/cấu hình; cấu hình mới có trong mục 6.

**Frontend**
- [ ] `npm run build` và `npm test` xanh; không `any`.
- [ ] Có trạng thái tải/rỗng/lỗi; hộp xác nhận cho thao tác nguy hiểm.
- [ ] Quyền theo vai trò đúng; lỗi hiển thị `errorCode` từ API.

**Chung**
- [ ] Commit đúng quy ước; PR điền mẫu, ghi mã Jira và FR.
- [ ] Chạy được bằng `docker compose up -d --build`.
- [ ] RTM và tài liệu liên quan đã cập nhật.
