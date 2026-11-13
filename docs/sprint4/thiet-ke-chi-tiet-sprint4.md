# TÀI LIỆU THIẾT KẾ CHI TIẾT SPRINT 4
**Dự án:** OISM (Omnichannel Inventory and Sales Management System)  
**Nhóm phụ trách:** Backend (M2, M3, M4, M5) & Frontend (M6, M7), QA/Lead (M1)  
**Ngày phát hành:** 04/11/2026  

---

## 1. Mục tiêu Sprint 4
Sprint 4 tập trung vào hoàn thiện vòng đời bán hàng tại quầy (**POS**), tích hợp thời gian thực (**Realtime STOMP WebSocket**), các tác vụ nền (**Scheduled background jobs**), cảnh báo tồn kho thấp (**Low stock alerts**) và các báo cáo phân tích nâng cao (**Định giá tồn kho, Top bán chạy, Hàng tồn chậm, Chốt sổ ngày**).

---

## 2. Kiến trúc & Công nghệ bổ sung

### 2.1 WebSocket STOMP (M2)
- **Endpoint kết nối:** `/ws` (STOMP qua WebSocket thuần; origin lấy từ `OISM_CORS_ORIGINS`).
- **Cơ chế xác thực (`TenantChannelInterceptor`):** frame `CONNECT` bắt buộc có `Authorization: Bearer <JWT>` hợp lệ, nếu không bị từ chối (client nhận frame ERROR). `SUBSCRIBE` chỉ được vào `/topic/tenants/{tenantId của token}/...`; nghe kênh tenant khác bị từ chối. Client không được `SEND`.
- **Kênh thông báo theo Tenant:**
  - `/topic/tenants/{tenantId}/orders`: Đơn hàng mới (đúng kênh trong Danh sách API của kế hoạch).
  - `/topic/tenants/{tenantId}/alerts`: Cảnh báo tồn kho thấp hoặc phục hồi.
- **Sự kiện giao dịch:** Sử dụng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` đảm bảo chỉ phát tin sau khi cơ sở dữ liệu đã commit thành công.

### 2.2 Thanh toán POS trong một giao dịch (M5)
- **Endpoint:** `POST /api/v1/pos/checkout` (`CASHIER`, `OWNER` theo kế hoạch). Cashier chỉ bán tại chi nhánh được gán, sai → 403 `AU40301` (FR-AUTH-05).
- **Nguyên tử hóa:**
  1. Validate chi nhánh & trạng thái SKU.
  2. Tạo đơn bán hàng `POS` (`DRAFT`).
  3. Khóa dòng tồn `stock_balance` bằng `@Lock(PESSIMISTIC_WRITE)` và giữ hàng (`RESERVED`).
  4. Trừ `on_hand`, giảm `reserved`, chụp `cost_price` từ `avg_cost` vào `order_item` (`CONFIRMED`).
  5. Chuyển sang `COMPLETED`.
  6. Ghi nhận thanh toán `payment` (`CASH` tính tiền thối hoặc `QR`).
  7. Cảnh báo tồn thấp được đánh giá ngay trong bước trừ kho (sổ cái), cùng giao dịch.
  8. Phát sự kiện `OrderCreatedEvent` cho WebSocket (sau commit). Đơn webhook, Simulator và đơn tạo tay cũng phát sự kiện này.
  Nếu bất kỳ bước nào gặp lỗi (hết hàng, số lượng không đủ), toàn bộ giao dịch rollback hoàn toàn.

### 2.3 Quản lý cảnh báo tồn kho thấp (M4)
- **Bảng `stock_alert`** (tạo ở V6, đưa về đúng mô hình dữ liệu của kế hoạch ở V9):
  - Cột theo kế hoạch: `branch_id`, `sku_id`, `available`, `threshold`, `status`, `closed_at`.
  - Trạng thái: `OPEN`, `CLOSED` (`CHECK ck_stock_alert_status`).
  - Mỗi (chi nhánh, SKU) tối đa 1 cảnh báo `OPEN`: unique index một phần `uq_stock_alert_open`.
- **Quy tắc (`StockAlertService.checkStockLevel`):**
  - Khi $\text{available} \le \text{reorder\_point}$ (và $\text{reorder\_point} > 0$): mở cảnh báo `OPEN` nếu chưa có.
  - Khi $\text{available} > \text{reorder\_point}$ (hoặc `reorder_point = 0`): tự động đóng (`CLOSED`).
  - Được gọi sau **mọi** thay đổi tồn, trong cùng giao dịch, sau khi đã khóa dòng `stock_balance`: `InventoryLedgerService.post` (nhập, bán, trả, chuyển kho, kiểm kê) và `ReservationService.reserve/release` (giữ/nhả hàng, hủy đơn, quá hạn).
  - Phát `StockAlertEvent` (OPEN hoặc CLOSED) sau commit lên `/topic/tenants/{tenantId}/alerts`.

### 2.4 Keyset Pagination cho Sổ cái (M4)
- Thay vì dùng `OFFSET` suy giảm hiệu năng trên hàng triệu bản ghi, hệ thống hỗ trợ truy vấn:
  $$\text{createdAt} < \text{lastCreatedAt} \lor (\text{createdAt} = \text{lastCreatedAt} \land \text{id} < \text{lastId})$$
  kết hợp `ORDER BY createdAt DESC, id DESC LIMIT N`.

### 2.5 Tác vụ nền định kỳ (M5)
- `ReservationExpirationJob`: Chạy mỗi 60 giây, quét các đơn `RESERVED` có `reserved_until < NOW()`, tự động hủy đơn và hoàn trả hàng khả dụng.
- `DailyReportJob`: Chạy mỗi đêm lúc 02:00 UTC, tổng hợp số đơn, doanh thu, giá vốn (COGS), lợi nhuận gộp của đơn CONFIRMED/COMPLETED theo chi nhánh × kênh vào `daily_report`; chạy lại cùng ngày thì ghi đè.

---

## 3. Thiết kế Cơ sở dữ liệu Sprint 4 (Flyway V6)
```sql
-- 1. Bảng cảnh báo tồn kho thấp
CREATE TABLE IF NOT EXISTS stock_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    threshold INT NOT NULL,
    available_at_alert INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

-- 2. Bảng chốt sổ báo cáo ngày: tạo lại ở V10 theo mô hình dữ liệu của kế hoạch
--    (1 dòng = chi nhánh × ngày × kênh; chỉ tính đơn CONFIRMED/COMPLETED)
CREATE TABLE daily_report (
    id           UUID           PRIMARY KEY,
    tenant_id    UUID           NOT NULL REFERENCES tenant (id),
    branch_id    UUID           NOT NULL REFERENCES branch (id),
    report_date  DATE           NOT NULL,
    channel      VARCHAR(20)    NOT NULL,
    order_count  INTEGER        NOT NULL DEFAULT 0,
    revenue      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    cogs         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    gross_profit NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_report_tenant_branch_date_channel UNIQUE (tenant_id, branch_id, report_date, channel)
);
```

---

## 4. Giao diện Frontend Admin & POS (M6 & M7)
- **Admin Portal:**
  - Trang Tổng quan Dashboard: 4 thẻ thống kê doanh thu, lợi nhuận gộp, đơn chờ xử lý, số cảnh báo; danh sách cảnh báo cần xử lý và Top 5 bán chạy.
  - Trang Báo cáo tồn kho (Owner, Staff): `GET /reports/inventory-value`, `/reports/top-selling`, `/reports/slow-moving`; tab Lịch sử chốt ngày chỉ Owner.
  - Quản lý Chi nhánh/Kho, Nhân viên và Cài đặt thời hạn giữ hàng.
  - Tích hợp thông báo chuông (Web Audio API) và popup khi có đơn hàng mới từ WebSocket.
- **POS App:**
  - Bổ sung màn hình thanh toán tiền mặt (tính tiền thối tự động, gợi ý tiền nhanh) và QR Code.
  - Xem và in hóa đơn khổ giấy 80mm ngay sau khi hoàn tất.
  - Đăng ký Service Worker và PWA Web App Manifest cho phép cài đặt và chạy ứng dụng độc lập.

