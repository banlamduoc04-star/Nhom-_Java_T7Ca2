# Kế hoạch kiểm thử v1 — OISM

Người phụ trách: M1. Phiên bản v1 (Sprint 2), cập nhật mỗi sprint. Đi kèm: [Test case v1](test-case-v1.md), [RTM v2](rtm-v2.md), [Báo cáo test thủ công API Sprint 1](bao-cao-test-api-sprint1.md).

## 1. Phạm vi

| Sprint | Trong phạm vi | Ngoài phạm vi (làm ở sprint sau) |
|---|---|---|
| 2 | AUTH (đăng ký, đăng nhập, phân quyền, chi nhánh, nhân viên), PROD, INV (phiếu nhập, sổ cái), COST, ORD (tạo đơn Draft, state machine), cô lập tenant, giao diện Admin PROD/INV, giao diện POS tìm kiếm + giỏ hàng | Giữ/trừ/nhả hàng, webhook, simulator (Sprint 3); checkout POS, realtime, báo cáo (Sprint 4); E2E, hiệu năng (Sprint 5) |
| 3 | + ORD, RSE, COST (chụp giá vốn), chuyển kho, kiểm kê, báo cáo lợi nhuận, Flash Sale | |
| 4 | + POS trọn vòng, WebSocket, tác vụ nền, báo cáo tồn, cảnh báo | |
| 5 | + E2E Playwright, JMeter, coverage ≥ 80%, bản Docker | |

## 2. Chiến lược và công cụ

| Tầng | Công cụ | Người viết | Khi nào chạy |
|---|---|---|---|
| Unit (domain, service) | JUnit 5, Mockito | Người làm module | Mỗi commit (`./mvnw test`) |
| Kiến trúc | ArchUnit | M2 | Mỗi commit, CI |
| Tích hợp API (DB thật, bảo mật thật) | `@SpringBootTest`, Testcontainers PostgreSQL 16, MockMvc | Người làm module | Mỗi PR (`./mvnw verify`), CI |
| Đồng thời | `ExecutorService` + Testcontainers | M4, M5 | Mỗi PR |
| API thủ công/hồi quy | Postman (M3 làm collection), Swagger | M1, M3 | Cuối sprint |
| Frontend unit | Vitest | M6, M7 | Mỗi PR |
| E2E giao diện | Playwright | M7 | Sprint 5 |
| Hiệu năng | JMeter, `EXPLAIN ANALYZE` | M4, M5, M7 | Sprint 5 |
| Coverage | JaCoCo (`-Pcoverage`) | M2 | CI, chặn merge từ Sprint 5 |

Nguyên tắc: kiểm thử ở tầng thấp nhất đủ chứng minh được quy tắc. Quy tắc tiền/tồn kho/phân quyền/tenant **bắt buộc** có test tích hợp trên PostgreSQL thật (không dùng H2) vì phụ thuộc khóa dòng, trigger và ràng buộc của PostgreSQL.

## 3. Môi trường và dữ liệu

- Máy dev và CI: JDK 17, Docker (Testcontainers tự dựng PostgreSQL 16 mỗi lần chạy, không cần cài DB).
- Môi trường thủ công: `docker compose up -d --build` với `OISM_SEED_ENABLED=true` (xem [Hướng dẫn chạy](../huong-dan/huong-dan-chay-du-an.md)).
- Dữ liệu mẫu (seed): 2 tenant, 3 chi nhánh (tenant A), 50 SKU, 3 nhà cung cấp, tài khoản đủ 3 vai trò, 2 phiếu nhập đã xác nhận; tenant B có 1 sản phẩm. Test tích hợp tự tạo dữ liệu riêng theo từng test (email ngẫu nhiên), không dùng seed.
- Dữ liệu tính tay cho giá vốn: 11 ví dụ trong `CostCalculatorTest`; Flash Sale: 50 luồng, 1 SKU còn 1 cái.

## 4. Tiêu chí

**Vào test:** build xanh, migration chạy sạch trên DB trống.

**Hoàn thành một hạng mục** (theo mục 8 kế hoạch dự án): biên dịch không lỗi; có unit test; API có Swagger; trả lỗi đúng `{errorCode, message}` và mã HTTP; đã có người khác duyệt; RTM cập nhật.

**Ra khỏi sprint:** mọi test tự động xanh trên CI; kịch bản nghiệm thu của sprint chạy được khi demo; lỗi mức Cao/Nghiêm trọng đóng hoặc có kế hoạch rõ.

Nghiệm thu Sprint 2 (từ kế hoạch): tạo sản phẩm 3 biến thể → nhập 2 phiếu giá khác nhau → `avg_cost` đúng công thức, sổ cái có 2 dòng IN với `balance_after` đúng; `UPDATE` trực tiếp vào sổ cái bị từ chối; CI xanh. Kịch bản này được tự động hóa trong `InventoryIT.sprint2Acceptance_twoReceiptsGiveWeightedAverageCostAndLedgerRows` và `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel`.

## 5. Quản lý lỗi

- Ghi lỗi trên GitHub Issues, nhãn theo module (`auth`, `prod`, `inv`, `ord`, `pos`, `admin`) và mức độ.
- Mỗi lỗi có: bước tái hiện, kết quả thực tế, kết quả mong đợi, mã lỗi API nếu có, ảnh/log, mã test case (`TC-...`).
- Mức độ: **Nghiêm trọng** (mất/sai dữ liệu tiền–tồn–tenant, lộ dữ liệu chéo tenant) · **Cao** (chức năng chính hỏng) · **Trung bình** (có cách tránh) · **Thấp** (giao diện, chính tả).
- Quy trình: Mở → Đã gán → Đã sửa (PR) → Kiểm lại → Đóng. Người sửa thêm test chặn tái phát.

## 6. Rủi ro

| Rủi ro | Cách giảm |
|---|---|
| Test tích hợp chậm, cần Docker | Dùng một PostgreSQL dùng chung cho mọi lớp `*IT`; chạy unit riêng bằng `./mvnw test` |
| Trùng số migration khi làm song song | Quy ước ở Chuẩn cấu hình chung, mục 5 |
| Test đồng thời không ổn định | Dùng `CountDownLatch` bắt đầu cùng lúc, không `Thread.sleep` |
| Docker Engine mới từ chối client cũ | Ghim Testcontainers 1.21.3 và `api.version=1.44` |
| JaCoCo lỗi trên đường dẫn có dấu | Chỉ bật trong profile `coverage` (CI chạy đường dẫn không dấu) |

## 7. Kế hoạch theo sprint

| Sprint | Việc kiểm thử chính |
|---|---|
| 2 | Test case AUTH/PROD/INV (≥ 30, đã có 44), test tự động tầng service + tích hợp, ArchUnit, test cô lập tenant, RTM v2, kiểm thử API Sprint 1 |
| 3 | Test case ORD/RSE/COST/chuyển kho/kiểm kê, **kịch bản Flash Sale** qua API, test ACID (lỗi giữa chuỗi tạo đơn → giữ → trừ), Postman REP |
| 4 | UAT đầu–cuối (nhập hàng → bán POS → đơn sàn → báo cáo), kiểm thử WebSocket, job nền |
| 5 | Coverage ≥ 80% lõi, E2E 3 kích thước màn hình, đo p95 < 200 ms, báo cáo < 2 s, sổ cái 1 triệu dòng < 500 ms, Postman cuối |
