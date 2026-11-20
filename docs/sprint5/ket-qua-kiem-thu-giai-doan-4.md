# Kết quả kiểm thử phi chức năng – Giai đoạn 4 (Sprint 5)

**Ngày chạy:** 11/11/2026. **Nhánh:** `feature/sprint5-sua-loi`. **Môi trường:** bản Docker dựng từ nhánh (`docker compose --profile frontend up -d --build`), PostgreSQL 16, backend Java 17. Số liệu dưới đây là kết quả chạy thật, không ước lượng.

## 1. Tổng hợp

| Hạng mục (theo kế hoạch) | Kết quả |
|---|---|
| Unit test + ArchUnit (`./mvnw test`) | 115/115 đạt |
| Test tích hợp Testcontainers (`./mvnw verify`) | 55/55 đạt |
| Coverage module lõi (NFR-MAINT-02, cổng JaCoCo trong CI) | Sổ cái 95,8% · Giữ hàng 84,1% · Giá vốn 100% (ngưỡng 80%) · báo cáo lợi nhuận và truy vấn tổng hợp 100% · toàn backend 84,7% |
| Flash Sale 50 đơn / 1 cái (NFR-PERF-02) | 1 thành công, 49 lỗi `IV40901`, `available` = 0 – qua cả service (`FlashSaleIT`) và API Simulator |
| Hoàn tác giao dịch (NFR-SEC-02) | Lỗi ở bước Giữ, Trừ, ghi thanh toán đều không để lại đơn/thanh toán/`reserved`/sổ cái dở (`TransactionRollbackIT`) |
| Healthcheck (NFR-USA-01) | 4 container `healthy`; `/actuator/health` chỉ trả `UP` |
| Postman (Sprint 5 M3) | 149 request, 244 assertion, 0 lỗi (2 collection, chạy bằng newman trên Docker) |
| E2E Playwright (Sprint 5 M7, NFR-USA-02) | 7/7 đạt: Admin desktop + POS ở 1366×768, 768×1024, điện thoại Pixel 7 |
| Hiệu năng (NFR-PERF-01, NFR-TENANT-02) | Tất cả chỉ tiêu đạt (mục 3) |

## 2. Chức năng được chứng minh bằng E2E

- **Admin (desktop):** đăng nhập → tạo sản phẩm → tạo và xác nhận phiếu nhập (tồn = 10) → gửi đơn Shopee qua webhook → **popup "Có đơn hàng mới" hiện trên Admin không cần tải lại trang** (FR-SIM-02) → duyệt đơn (trừ kho).
- **POS (3 kích thước):** quét mã vạch + Enter 2 lần (số lượng 2) → `F9` → `500k` → "Xác nhận & In bill" (3 thao tác sau khi quét) → hóa đơn hiện, **hộp thoại in tự mở** (FR-POS-03) → tồn còn 8, `reserved` 0; không tràn ngang màn hình. Thêm vượt tồn bị chặn và báo "chỉ còn 1" (FR-POS-02).

## 3. Hiệu năng

Dữ liệu: mỗi tenant đo có **1.000.000 dòng sổ cái** (3 chi nhánh × 20 SKU, rải 365 ngày) và **100.000 đơn** Confirmed/Completed (200.000 dòng hàng, 90 ngày); lúc đo bảng có tổng 3 triệu dòng sổ cái và 300.000 đơn của 3 tenant. Thời gian API là p95 của 30 lần gọi (báo cáo: 10 lần) sau 3 lần khởi động, đo từ máy chủ Docker cục bộ.

| Truy vấn / API | p95 | Chỉ tiêu | Kết quả |
|---|---|---|---|
| Quét mã vạch `GET /skus/lookup` | 32,6 ms | < 200 ms | Đạt |
| Tìm SKU `GET /skus?keyword=` | 37,6 ms | < 200 ms | Đạt |
| Sổ cái trang 1 `GET /inventory-transactions` | 150,4 ms | < 500 ms | Đạt |
| Sổ cái lọc SKU | 46,7 ms | < 500 ms | Đạt |
| Sổ cái keyset | 31,8 ms | < 500 ms | Đạt |
| Tạo đơn `POST /orders` | 33,9 ms | < 500 ms | Đạt |
| Báo cáo lợi nhuận (100.000 đơn) | 352,1 ms | < 2 s | Đạt |
| Giá trị tồn / Top bán chạy 90 ngày / Tồn chậm | 33,1 / 129,8 / 74,9 ms | < 2 s | Đạt |

`EXPLAIN ANALYZE` (script `backend/scripts/explain_ledger_queries.sql`) trên 1 triệu dòng của một tenant:

| Truy vấn | Kế hoạch thực thi | Thời gian |
|---|---|---|
| Trang đầu sổ cái | Index Scan Backward `ix_invtx_tenant_created` | 0,13 ms |
| Đếm tổng cho phân trang | Parallel Index Only Scan `ix_invtx_tenant_sku` | 41 ms |
| Lọc theo SKU | Index Scan Backward `ix_invtx_tenant_created` | 0,24 ms |
| Keyset lùi sâu 200 ngày | Index Scan Backward `ix_invtx_tenant_created_id` (điều kiện `ROW(created_at, id) <`) | 0,12 ms (trước V12: 217 ms) |
| Khoảng 7 ngày | Index Scan Backward `ix_invtx_tenant_created` | 0,05 ms |

Cách chạy lại: tạo tenant qua API, rồi `psql -v tenant_id=<uuid> -f backend/scripts/generate_1m_ledger.sql`, `generate_100k_orders.sql`, `explain_ledger_queries.sql` (chỉ trên CSDL kiểm thử).

## 4. Lỗi phát hiện và đã sửa trong giai đoạn này

| Lỗi | Ảnh hưởng | Sửa |
|---|---|---|
| `GET /inventory-transactions/keyset` trang đầu luôn 500 (`could not determine data type of parameter`) | Phân trang keyset Sprint 4 chưa từng chạy | Tách truy vấn trang đầu/trang sau, so sánh bộ `(created_at, id)`; V12 thêm index `(tenant_id, created_at, id)`; `LedgerKeysetIT` |
| Báo cáo lợi nhuận, top bán chạy, tồn chậm, chốt ngày nạp mọi đơn vào bộ nhớ rồi `IN (...)` | **500 khi > 65.535 đơn** – không chạy được ở quy mô NFR-PERF-01 | Tổng hợp trong CSDL (`SalesAnalyticsRepository`, `SUM … GROUP BY`); `ProfitReportIT` chèn 70.000 đơn |
| Báo cáo lợi nhuận trả `{totalRevenue, rows[]}` nhưng Admin đọc `{summary, bySku}` | Màn Báo cáo lợi nhuận và thẻ doanh thu Dashboard không có số liệu | Trả `{summary, bySku}` theo SKU; CSV chi tiết từng dòng, số không phụ thuộc locale |
| Admin kiểm kê gửi `countedQty`, backend nhận `countedQuantity` (int → mặc định 0) | **Mọi số đếm trên Admin thành 0**, duyệt sẽ ghi điều chỉnh sai | Kiểu dữ liệu Admin cho kiểm kê/chuyển kho khớp record backend |
| Healthcheck frontend dùng `localhost` (= `::1` trong nginx-alpine, nginx chỉ nghe IPv4) | Container Admin/POS luôn `unhealthy` | Đổi sang `127.0.0.1` |
| Backend không có actuator, `/actuator/health` 404 | Không có healthcheck backend (NFR-USA-01) | Thêm actuator chỉ mở `health`, `HEALTHCHECK` trong Dockerfile và compose |
| `generate_1m_ledger.sql` dùng lý do `BENCHMARK_STRESS_TEST` và `created_by` chuỗi | Script không chạy được | Viết lại, nhận `tenant_id`, lý do hợp lệ |
| `CostCalculator.calculateMovingAverageCost` (4 chữ số) – code chết của sổ cái thứ hai | Kéo coverage giá vốn xuống 53%, trái FR-COST-01 | Xóa |
| Collection Postman M3: đăng ký email có timestamp nhưng đăng nhập email cố định; `emptyBrandId` không được gán; đọc `totalValuation` | 69/77 assertion lỗi dây chuyền | Sửa biến và assertion; nay 77/77 đạt |

## 5. Chưa làm / giới hạn

- E2E và Postman cần bản Docker đang chạy, **chưa đưa vào CI** (CI hiện chạy build, unit, tích hợp, cổng coverage).
- Số đo hiệu năng trên một máy phát triển (Docker Desktop, Windows); chưa đo bằng JMeter/tải song song nhiều người dùng.
