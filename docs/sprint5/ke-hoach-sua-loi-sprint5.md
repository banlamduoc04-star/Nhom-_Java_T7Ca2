# Kế hoạch sửa lỗi Sprint 5 – đưa code về đúng kế hoạch dự án

**Nguồn sự thật:** `docs/ke-hoach/ke-hoach-du-an-oism.md` (gọi tắt là **KH**). Quy ước code: `docs/huong-dan/chuan-cau-hinh-chung.md` (gọi tắt là **Chuẩn**).
**Nguyên tắc:** code khác KH thì sửa code; tài liệu sprint khác KH thì sửa tài liệu. Chỉ thêm thứ KH không có khi bắt buộc, ghi "(bổ sung)" và cập nhật lại KH.
**Ngày lập:** 10/11/2026. **Trạng thái:** Giai đoạn 1–3 xong (11/11/2026); giai đoạn 4–5 chưa làm.

## 0. Cách làm

- Nhánh `feature/sprint5-sua-loi` tách từ `origin/develop` (Chuẩn mục 8). Mỗi giai đoạn dưới đây là một nhóm commit `fix(...)`/`feat(...)` riêng.
- `develop` đã có sẵn `V7__add_lazada_and_ledger_tables.sql`, nên migration của đợt sửa bắt đầu từ **V8**. Không sửa V1–V7.

### 0.1. Hiện trạng `develop` khi bắt đầu (11/11/2026), cần quyết định riêng

| Vấn đề | Lệch kế hoạch ở đâu | Đề xuất |
|---|---|---|
| Kênh `LAZADA` (enum + CHECK trong V7) | KH chỉ có POS/MANUAL/SHOPEE/TIKTOK | **Đã bỏ (quyết định 11/11/2026):** `V11__bo_kenh_lazada.sql` khôi phục CHECK theo KH, đơn LAZADA cũ chuyển về MANUAL; gỡ khỏi enum, seeder, Admin |
| Bảng `inventory_items`, `ledger_entries` + `InventoryService`, `LedgerService`, `ReportingService`, `JpaReportRepository` (V7) | `id BIGSERIAL`, `tenant_id VARCHAR`, sổ cái thứ hai song song với `inventory_transaction`; trái mô hình dữ liệu và quyết định kiến trúc 2 của KH | **Đã gỡ ở giai đoạn 3** (V10 DROP TABLE; xóa code và 2 test đi kèm) |
| 3 test tích hợp đặt tên `*Test` nên chạy dưới Surefire và lỗi Docker | Chuẩn mục 3/9: test tích hợp tên `*IT` | **Đã sửa:** đổi tên `FlashSaleIT`, `InventoryEntityConcurrencyIT`, `LedgerQueryPerformanceIT`; sửa tham số đảo trong `FlashSaleIT` |
- Mỗi mục sửa phải có test: 1 case đúng + 1 case lỗi; API mới có test 2 tenant → 404 (Chuẩn mục 9, NFR-TENANT-01).
- Kết thúc mỗi giai đoạn: `./mvnw verify` xanh (cần Docker), `npm run build` + `npm test` xanh cho 2 frontend.

## Giai đoạn 1 – Bảo mật (ưu tiên cao nhất)

| # | Việc | Theo KH | File chính |
|---|---|---|---|
| 1.1 | Tạo bảng `channel_credential` (`id`, `tenant_id`, `channel` SHOPEE/TIKTOK, `api_key_hash`, `active`, `created_at`, `updated_at`), unique `api_key_hash` | Mô hình dữ liệu, dòng AUTH `channel_credential` | `V8`, `domain/model/ChannelCredential`, repository |
| 1.2 | Webhook: đọc header **`X-Api-Key`** (KH ghi `X-Api-Key`, code đang dùng `X-Webhook-Key`), băm SHA-256, tra `channel_credential` để lấy `tenantId`; sai/thiếu/không khớp kênh → 401 `OD40101`. Xóa khóa cứng `oism-webhook-secret-key` và header `X-Tenant-Id` | FR-SIM-01; Cô lập tenant: "Webhook lấy tenantId từ API key" | `WebhookController`, `application/order/WebhookAuthService` (mới) |
| 1.3 | (bổ sung) Owner sinh/xoay API key theo kênh: `POST /settings/channel-keys/{channel}` trả key thô **một lần**; seeder tạo sẵn key demo | Cần để FR-SIM-01 dùng được | `SettingsController`, `TenantSettingsService`, `DemoDataSeeder`, `SettingsPage.tsx` |
| 1.4 | WebSocket: từ chối `CONNECT` không có JWT hợp lệ; chặn `SUBSCRIBE` vào `/topic/tenants/{id}/...` khác `tenantId` trong token; origin lấy từ `OISM_CORS_ORIGINS` thay cho `*` | FR-SIM-02 "kênh riêng theo tenant_id"; NFR-TENANT-01 | `WebSocketConfig` |
| 1.5 | Đổi kênh về đúng KH: `/topic/tenants/{tenantId}/orders` (đang là `/orders/new`) và `/alerts` | Danh sách API, dòng SIM WebSocket | `RealtimeEventListener`, frontend |
| 1.6 | Sửa XSS phiếu giao: bỏ `document.write` chuỗi ghép, dựng phiếu bằng component React (tự escape) từ API 3.1 | FR-ORD-03 "in phiếu giao hàng" | `OrdersPage.tsx`, component `ShippingSlip` (mới) |
| 1.7 | POS chỉ cho **Cashier, Owner** (bỏ STAFF); Cashier chỉ bán tại `branchId` được gán, sai → 403 `AU40301` | Danh sách API dòng POS; FR-AUTH-05 | `PosController`, `PosCheckoutService` |
| 1.8 | Simulator chỉ **Owner** (bỏ STAFF) | Danh sách API dòng SIM | `SimulatorController`, menu Admin |

**Kết quả giai đoạn 1 (11/11/2026):**

| # | Trạng thái | Ghi chú |
|---|---|---|
| 1.1 | Xong | `V8__channel_credential.sql`, `ChannelCredential`, `ChannelCredentialRepository`; unique `api_key_hash` và 1 key active / tenant × sàn |
| 1.2 | Xong | `WebhookApiKeyFilter` xác thực trước validation; thiếu/sai/sai sàn/dùng JWT → 401 `OD40101`; bỏ khóa cứng và `X-Tenant-Id` |
| 1.3 | Xong (không seed key) | `GET/POST /settings/channel-keys`; mục "API key webhook sàn" ở trang Cài đặt; Simulator có ô nhập key. Seeder **không** tạo key để tránh đưa key ra log; Owner tự sinh |
| 1.4 | Xong | `TenantChannelInterceptor`: CONNECT bắt buộc JWT, SUBSCRIBE đúng tenant, chặn SEND; origin theo `OISM_CORS_ORIGINS`; bỏ SockJS |
| 1.5 | Xong | Kênh `/topic/tenants/{id}/orders` (backend + Admin). Admin vẫn kết nối sai host → giai đoạn 2.2 |
| 1.6 | Không cần sửa | `develop` đã escape mọi trường khi in phiếu (`escapeHtml`); API `shipping-slip` vẫn làm ở 3.1 |
| 1.7 | Xong | POS: Cashier + Owner; Cashier khác chi nhánh → 403 `AU40301` (kiểm tra trong `PosCheckoutService`) |
| 1.8 | Xong | Simulator chỉ Owner |
| Thêm | Xong | Sửa 2 lỗi có sẵn trên `develop`: Simulator gửi sai body (`items` thay vì `skuId/quantity`, đọc `requested` thay vì `total`) nên luôn 400; trang Cài đặt gọi `update(số)` nên lưu luôn lỗi |

Kiểm chứng: `./mvnw verify` xanh: 96 unit test + 38 test tích hợp (mới: `WebhookIT` 7, `PosCheckoutIT` 2, `TenantChannelInterceptorTest` 6, `PosCheckoutServiceTest` +2). Admin/POS `npm run build`, `npm run lint`, `npm test` xanh.

## Giai đoạn 2 – Realtime và cảnh báo tồn (để UAT Sprint 4 đạt thật)

| # | Việc | Theo KH |
|---|---|---|
| 2.1 | Phát `OrderCreatedEvent` sau commit ở **mọi** luồng tạo đơn: webhook, simulator, đơn tay, POS | Luồng giữ hàng bước 6 "Commit, sau đó phát sự kiện WebSocket đơn mới"; nghiệm thu Sprint 4 |
| 2.2 | Admin: kết nối WebSocket tới **backend** (suy ra từ `VITE_API_URL`, không dùng `window.location.host`), lấy token từ `tokenStore` (khóa `oism.admin.tokens`); dùng thư viện `@stomp/stompjs` thay cho frame tự ghép | FR-SIM-02 |
| 2.3 | POS: nhận thông báo đơn mới qua WebSocket | Sprint 4, M7 việc (5) |
| 2.4 | Kiểm tra cảnh báo tồn sau **mọi** thay đổi `available`: xác nhận phiếu nhập, chuyển kho (xuất/nhận), duyệt kiểm kê, giữ/nhả/xác nhận/hủy đơn, POS. Gọi trong cùng giao dịch, **bỏ** `try/catch (Exception)` ở `PosCheckoutService` | FR-REP-03 "tự đóng khi nhập thêm vượt ngưỡng"; Chuẩn mục 3 |
| 2.5 | Trạng thái cảnh báo theo KH: `OPEN/CLOSED` (đổi `RESOLVED` → `CLOSED`, cập nhật dữ liệu cũ trong migration V9); cột `available` theo KH (đổi tên `available_at_alert`) | Mô hình dữ liệu dòng `stock_alert` |
| 2.6 | Unique index một phần `uq_stock_alert_open (tenant_id, branch_id, sku_id) WHERE status = 'OPEN'` + `CHECK (status IN ('OPEN','CLOSED'))`; đổi tên index `idx_` → `ix_` | "Mỗi (branch_id, sku_id) tối đa 1 cảnh báo OPEN"; Chuẩn mục 5 |
| 2.7 | Thay `findById` bằng hàm có `tenantId` trong `StockAlertService` | Chuẩn mục 7.1.3 |

**Kết quả giai đoạn 2 (11/11/2026):**

| # | Trạng thái | Ghi chú |
|---|---|---|
| 2.1 | Xong | `OrderService.createDraft/createAndReserve` phát `OrderCreatedEvent`; listener `AFTER_COMMIT` nên đơn rollback không được thông báo |
| 2.2 | Xong | Admin nối `ws://<host API>/ws` (`websocketUrl()` suy ra từ `VITE_API_URL`), đọc token mới nhất trước mỗi lần (re)connect; cảnh báo `CLOSED` chỉ cập nhật số đếm, không báo động |
| 2.3 | Xong | POS (`src/realtime.ts`, thêm `@stomp/stompjs`) hiện dải "Đơn mới …" 8 giây; `Me` thêm `tenantId`; tắt khi chạy dữ liệu giả |
| 2.4 | Xong | `checkStockLevel` gọi trong `InventoryLedgerService.post` và `ReservationService.reserve/release` (mọi thay đổi `on_hand`/`reserved`), `Propagation.MANDATORY`; bỏ `try/catch (Exception)` và phụ thuộc `StockAlertService` khỏi `PosCheckoutService` |
| 2.5 | Xong | `V9__stock_alert_theo_ke_hoach.sql`: `RESOLVED` → `CLOSED`, đổi cột `available_at_alert` → `available`, `resolved_at` → `closed_at`; đóng bớt cảnh báo OPEN trùng trong dữ liệu cũ |
| 2.6 | Xong | `uq_stock_alert_open` + `ck_stock_alert_status`; index `ix_stock_alert_tenant_status/_sku/_created` |
| 2.7 | Xong | `StockAlertService` chỉ dùng truy vấn có `tenantId` (`findByIdAndTenantId`, `findByTenantIdAndIdIn`); `POS` cũng thôi dùng `findAllById` cho sản phẩm |
| Thêm | Xong | Admin gọi `POST /stocktakes/{id}/counts` trong khi KH và backend là `PUT` (405) → đổi sang `PUT` |

Kiểm chứng: `./mvnw verify` xanh: 100 unit test + 43 test tích hợp. Mới: `StockAlertIT` (3: kịch bản 2 UAT Sprint 4, đơn sàn giữ/hủy, cô lập tenant), `RealtimeIT` (2: client STOMP thật nhận đơn webhook + cảnh báo của tenant mình, tenant khác bị ERROR, không token bị từ chối), `OrderServiceTest` +2, `StockAlertServiceTest` +2. Admin/POS `build`, `lint`, `test` xanh. Chưa kiểm tra bằng mắt trên trình duyệt.

## Giai đoạn 3 – API, quyền và dữ liệu theo đúng KH

| # | Việc | Theo KH |
|---|---|---|
| 3.1 | Thêm `GET /orders/{id}/shipping-slip` (Owner, Staff) | Danh sách API dòng ORD |
| 3.2 | Thêm `GET /pos/orders/{id}/receipt` (Cashier, Owner) | Danh sách API dòng POS |
| 3.3 | Đổi đường dẫn báo cáo tồn: `/reports/inventory-value`, `/reports/top-selling`, `/reports/slow-moving`; quyền **Owner + Staff** (đang chỉ Owner) | Danh sách API dòng REP; FR-REP-02 |
| 3.4 | Cảnh báo: `GET /alerts/stock`, `PATCH /alerts/stock/{id}/read` (thay `POST .../resolve`) | Danh sách API dòng REP |
| 3.5 | `daily_report` theo KH: thêm `branch_id`, `channel`, `order_count`, `revenue`, `cogs`, `gross_profit`; unique `(tenant_id, branch_id, report_date, channel)`. Bảng chỉ chứa số liệu tổng hợp nên migration V9 tạo lại bảng và job tính lại | Mô hình dữ liệu dòng `daily_report`; FR-SIM-03 |
| 3.6 | Mã lỗi validation đúng module cho `/pos`, `/simulator` (OD), `/alerts` (IV), `/users`, `/branches`, `/settings` (AU) | Bảng mã lỗi |
| 3.7 | Thời gian giữ hàng mặc định 30 phút (`DEFAULT_RESERVE_DURATION` đang 15) | FR-RSE-02 |
| 3.8 | Lịch sử trạng thái đơn ghi đúng từng bước chuyển (`null→DRAFT→RESERVED`, POS: `…→CONFIRMED→COMPLETED`) | FR-ORD-02 "mỗi lần chuyển ghi lịch sử" |
| 3.9 | POS tự mở hộp thoại in sau khi thanh toán xong | FR-POS-03 |
| 3.10 | QR thanh toán sinh tại chỗ (thư viện JS), không gọi `api.qrserver.com` | FR-POS-03; không gửi dữ liệu ra dịch vụ ngoài |
| 3.11 | Bỏ `any` trong `StocktakesPage.tsx`, `TransfersPage.tsx`; sửa cảnh báo lint | Chuẩn mục 10 |

Các API **có trong code nhưng KH không có** (`/skus`, `/inventory-transactions/keyset`, `/transfers/{id}/cancel`, `/alerts/stock/count`, `/reports/daily`, `/users/{id}/password`, `/settings`) giữ nguyên, ghi "(bổ sung)" vào Danh sách API của KH.

**Kết quả giai đoạn 3 (11/11/2026):**

| # | Trạng thái | Ghi chú |
|---|---|---|
| 3.1 | Xong | `GET /orders/{id}/shipping-slip` (`ShippingSlipService`), có tên sản phẩm và chi nhánh; đơn đã hủy → 409 `OD40901`. Nút "In phiếu" trên Admin lấy dữ liệu từ API này, vẫn escape mọi trường |
| 3.2 | Xong | `GET /pos/orders/{id}/receipt`: in lại hóa đơn; không phải đơn POS → 404 `OD40401`; Cashier chỉ xem hóa đơn chi nhánh được gán |
| 3.3 | Xong | `/reports/inventory-value`, `/reports/top-selling`, `/reports/slow-moving` cho Owner + Staff; menu/route Admin mở cho Staff, tab chốt ngày chỉ Owner; Postman đã đổi đường dẫn |
| 3.4 | Xong | `PATCH /alerts/stock/{id}/read` (204) đóng cảnh báo; bỏ `POST .../resolve` |
| 3.5 | Xong | `V10__daily_report_theo_ke_hoach.sql`: dựng lại `daily_report` theo KH (chi nhánh × ngày × kênh, unique đúng KH); chỉ tính đơn Confirmed/Completed; chạy lại cùng ngày ghi đè (sửa lỗi cũ xóa rồi chèn trong cùng lần flush gây trùng unique); trả `DailyReportView` thay vì entity |
| 3.6 | Xong | `validationCode`: `/pos`, `/simulator` → `OD40001`; `/alerts` → `IV40001`; còn lại module AUTH → `AU40001` (13 case kiểm tra) |
| 3.7 | Xong | `DEFAULT_RESERVE_DURATION` = 30 phút |
| 3.8 | Không cần sửa | `develop` đã ghi lịch sử từng bước (`null→DRAFT→RESERVED`; POS 4 bước) |
| 3.9 | Xong | POS tự mở hộp thoại in 300 ms sau khi hóa đơn hiện |
| 3.10 | Xong | `QrCode.tsx` dùng thư viện `qrcode` sinh ảnh tại chỗ; bỏ `api.qrserver.com` |
| 3.11 | Xong | Bỏ toàn bộ `any` trong Admin (không chỉ 2 trang): lỗi hiển thị bằng `errorText(err)` như Chuẩn mục 10 |
| Thêm | Xong | Gỡ `GET /reports/dashboard-summary` (không lọc tenant → lộ doanh thu mọi cửa hàng, số liệu biểu đồ ghi cứng, `@CrossOrigin("*")`, không phân quyền) và sổ cái thứ hai của V7; gỡ bí danh công khai `POST /auth/register-tenant` (Chuẩn 4.4) |

Kiểm chứng: `./mvnw verify` xanh: 113 unit test + 45 test tích hợp (mới: `OrderDocumentsIT` 2, `ReportApiIT` 2, `StockAlertIT` thêm PATCH /read, 13 case mã lỗi validation; bỏ 2 test của sổ cái thứ hai). Admin/POS `build`, `lint`, `test` xanh. Chưa kiểm tra bằng mắt trên trình duyệt.

Kênh `LAZADA`: đã bỏ theo quyết định của nhóm (xem mục 0.1, migration V11).

## Giai đoạn 4 – Phi chức năng và kiểm thử (Sprint 5 trong KH)

| # | Việc | Theo KH |
|---|---|---|
| 4.1 | Thêm `spring-boot-starter-actuator` (chỉ mở `health`); `HEALTHCHECK` trong `backend/Dockerfile` và `docker-compose.yml` | NFR-USA-01; Sprint 5 M2 (5) |
| 4.2 | JaCoCo `check` ≥ 80% cho `InventoryLedgerService`, `ReservationService`, `CostCalculator`; CI đỏ khi dưới ngưỡng | NFR-MAINT-02; Sprint 5 M2 (1) |
| 4.3 | Sửa build arg trong compose: `NEXT_PUBLIC_API_URL` → `VITE_API_URL` | Chuẩn mục 6 |
| 4.4 | Test tích hợp mới: webhook (key sai 401 `OD40101`, trùng đơn `OD40902`, tenant lấy từ key), WebSocket (không token bị từ chối, subscribe chéo tenant bị chặn), cảnh báo tự đóng khi nhập hàng, cashier khác chi nhánh 403, shipping-slip/receipt chéo tenant 404 | Chuẩn mục 9 |
| 4.5 | **Flash Sale đúng KH:** 50 đơn đồng thời vào 1 SKU còn 1 cái → 1 thành công, 49 lỗi `IV40901`, `available` = 0 | NFR-PERF-02 |
| 4.6 | Test hoàn tác: lỗi giữa chuỗi Tạo đơn → Giữ → Trừ không để lại dữ liệu dở | NFR-SEC-02 |
| 4.7 | Postman collection + environment (mỗi endpoint 1 case đúng, 1 case lỗi, kiểm tra `errorCode`) | Sprint 5 M3 (2)(3) |
| 4.8 | Playwright E2E (đăng nhập, tạo sản phẩm, nhập kho, bán POS, đơn sàn) ở 3 kích thước màn hình; đo p95 tìm/quét < 200 ms | NFR-USA-02, NFR-PERF-01; Sprint 5 M7 |
| 4.9 | Đo báo cáo < 2 s và sổ cái 1 triệu dòng (`EXPLAIN ANALYZE`) bằng `scripts/generate_1m_ledger.sql` | NFR-PERF-01, NFR-TENANT-02 |

## Giai đoạn 5 – Sửa tài liệu cho khớp KH và kết quả thật

| Tài liệu | Sửa |
|---|---|
| `sprint3/thiet-ke-chi-tiet-sprint3.md` | `DISPATCHED` → `IN_TRANSIT`; bỏ `CANCELLED` của kiểm kê; thay SQL V5 bằng nội dung V5 thật |
| `sprint3/test-case-sprint3.md` | `POST .../counts` → `PUT`; lý do sổ cái `TRANSFER_OUT/TRANSFER_IN/RETURN/ADJUST`; 30 phút; bỏ Lazada |
| `sprint3/kich-ban-flash-sale.md` | Chạy lại theo 4.5 (50 đơn / 1 cái), ghi số liệu thật, mã đơn thật; bỏ Lazada |
| `sprint4/thiet-ke-chi-tiet-sprint4.md` | Kênh WebSocket, trạng thái `CLOSED`, cấu trúc `daily_report`, quyền POS theo KH |
| `sprint4/kich-ban-bao-cao-uat-sprint4.md` | Chạy lại kịch bản 2 và 3 sau giai đoạn 2; ghi kết quả thật |
| `sprint4/rtm-v4.md` → `sprint5/rtm-v5.md` | Trạng thái từng FR/NFR theo kết quả test thật, thêm cột test tương ứng |
| `sprint1/kien-truc-backend-sprint1.md` | Đường dẫn `backend/`, bỏ câu "chưa biên dịch", commit file |
| `huong-dan/chuan-cau-hinh-chung.md` mục 7.2.1 | Ghi đúng như KH: chỉ `on_hand` bắt buộc đi qua `InventoryLedgerService.post`; `reserved` sửa trong `ReservationService` dưới khóa `FOR UPDATE` |
| `ke-hoach/ke-hoach-du-an-oism.md` | Thêm các API "(bổ sung)" và mã lỗi mới (nếu có) |
| `README.md` | Thêm cây thư mục 4 lớp Clean Architecture (giải thích vì sao không phải `controller/service/repository`) |

## Thứ tự và tiêu chí xong

1 → 2 → 3 → 4 → 5. Hoàn tất khi đạt **Nghiệm thu Sprint 4 và Sprint 5 của KH**: gửi webhook từ Simulator thì Admin hiện popup không tải lại trang; nhập hàng vượt ngưỡng thì cảnh báo tự đóng; Flash Sale 50/1 đạt; `docker compose up` chạy đủ hệ thống, có healthcheck; CI xanh với coverage module lõi ≥ 80%; RTM v5 khớp kết quả test.
