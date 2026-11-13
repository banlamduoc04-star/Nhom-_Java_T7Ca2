# TÀI LIỆU THIẾT KẾ CHI TIẾT SPRINT 3
**Dự án:** OISM  
**Nhóm phụ trách:** Backend (M2, M3, M4, M5) & Frontend (M6, M7)  
**Ngày phát hành:** 04/11/2026 · **Cập nhật:** 11/11/2026 (khớp code và kế hoạch, Sprint 5)  

---

## 1. Kiến trúc tổng thể và Tuân thủ Clean Architecture
Hệ thống theo 4 lớp Clean Architecture (Kế hoạch mục 4, quyết định 1), được ArchUnit kiểm tra tự động trong CI (`ArchitectureTest`):
1. **Domain:** Entity (`SalesOrder`, `OrderItem`, `StockTransfer`, `Stocktake`, `StockBalance`), enum, interface repository (Spring Data) và quy tắc thuần (`OrderStateMachine`, `CostCalculator`). Không phụ thuộc lớp nào khác.
2. **Application:** Service use case (`OrderService`, `ReservationService`, `StockTransferService`, `StocktakeService`, `ProfitReportService`), record `...View` trả ra ngoài.
3. **Infrastructure:** Bảo mật (`SecurityConfig`, JWT, xác thực webhook), bộ lọc tenant, WebSocket, tác vụ nền, truy vấn tổng hợp báo cáo (`JpaSalesAnalyticsRepository`).
4. **Web:** REST controller (`OrderController`, `StockTransferController`, `StocktakeController`, `ProfitReportController`, `SimulatorController`, `WebhookController`), không gọi repository trực tiếp.

---

## 2. Thiết kế Cơ sở dữ liệu (Flyway V3, V5)

### 2.1 Bảng `stock_transfer` và `stock_transfer_item`
- `stock_transfer`: phiếu chuyển liên chi nhánh (`from_branch_id`, `to_branch_id`, `code`, `status`: `DRAFT` → `IN_TRANSIT` → `RECEIVED`, hoặc `DRAFT` → `CANCELLED`). Ràng buộc `ck_transfer_branches` (hai chi nhánh khác nhau). Chỉ hủy được phiếu còn `DRAFT`.
- `stock_transfer_item`: `transfer_id`, `sku_id`, `quantity > 0`.

### 2.2 Bảng `stocktake` và `stocktake_item`
- `stocktake`: phiên kiểm kê theo chi nhánh (`branch_id`, `code`, `status`: `OPEN` → `APPROVED`, `approved_by`). Phiên đã duyệt không sửa được (409 `IV40902`).
- `stocktake_item`: `system_qty` (chụp `on_hand` lúc mở phiên), `counted_qty`, `difference = counted_qty - system_qty`. API dùng tên `systemQuantity`, `countedQuantity`.

### 2.3 Index bổ sung ở V5 (NFR-TENANT-02)
Nội dung thật của `V5__sprint3_indexes.sql`:
```sql
-- V5: Sprint 3 bo sung index toi uu truy van theo tenant, thoi gian, chi nhanh (NFR-TENANT-02)

CREATE INDEX IF NOT EXISTS ix_order_item_tenant_created ON order_item (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS ix_orders_tenant_branch ON orders (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_orders_tenant_channel ON orders (tenant_id, channel);
CREATE INDEX IF NOT EXISTS ix_stock_balance_tenant_branch ON stock_balance (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_stock_transfer_tenant_status ON stock_transfer (tenant_id, status);
CREATE INDEX IF NOT EXISTS ix_stocktake_tenant_branch ON stocktake (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS ix_stocktake_item_tenant_sku ON stocktake_item (tenant_id, sku_id);
CREATE INDEX IF NOT EXISTS ix_stock_transfer_item_tenant_sku ON stock_transfer_item (tenant_id, sku_id);
```
Index `(tenant_id, created_at)` và `(tenant_id, sku_id)` của sổ cái và đơn hàng đã có từ V3, V4; V12 (Sprint 5) thêm `ix_invtx_tenant_created_id` cho phân trang keyset sổ cái.

---

## 3. Máy trạng thái Đơn hàng (Order State Machine)
Đơn hàng trải qua các trạng thái tuần tự và chặt chẽ:
```
DRAFT (Tạo nháp)
  │
  ├─(reserve)──> RESERVED (Giữ hàng)
  │                │
  │                ├─(confirm)──> CONFIRMED (Trừ on_hand, chụp avg_cost)
  │                │                │
  │                │                ├─(complete)──> COMPLETED (Hoàn tất)
  │                │                │
  │                │                └─(cancel)────> CANCELLED (Hoàn lại on_hand: sổ cái IN/RETURN)
  │                │
  │                └─(cancel / expired)──> CANCELLED (Nhả reserved, available phục hồi)
  │
  └─(cancel)───> CANCELLED
```

---

## 4. Công thức Báo cáo Lợi nhuận gộp (Profit Calculation)
Theo nguyên tắc kế toán dồn tích và phương pháp bình quân gia quyền:
1. **Doanh thu (Revenue):**
   $$\text{Revenue} = \sum (\text{item.quantity} \times \text{item.unit\_price})$$
2. **Giá vốn hàng bán (COGS):**
   $$\text{COGS} = \sum (\text{item.quantity} \times \text{item.cost\_price})$$
   *(Trong đó `cost_price` đã được chụp bất biến từ `avg_cost` tại thời điểm đơn hàng chuyển sang `CONFIRMED`).*
3. **Lợi nhuận gộp (Gross Profit):**
   $$\text{Gross Profit} = \text{Revenue} - \text{COGS}$$
4. **Tỷ suất lợi nhuận gộp (Gross Margin):**
   $$\text{Gross Margin} = \frac{\text{Gross Profit}}{\text{Revenue}} \times 100\%$$

**API (`GET /reports/profit`, Owner):** chỉ tính đơn `CONFIRMED`/`COMPLETED`; trả `{summary, bySku}`. Trong đó `summary` gồm `totalRevenue`, `totalCogs`, `grossProfit`, `grossMargin` (dạng tỷ lệ, 0,4 = 40%), `totalOrders`, `totalQuantity`; `bySku` là các dòng theo SKU xếp theo doanh thu giảm dần. Số liệu được tổng hợp ngay trong CSDL (`SUM … GROUP BY`) nên chạy được với 100.000 đơn (đo p95 352 ms, xem `docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md`). `GET /reports/profit/export` xuất CSV chi tiết từng dòng hàng, số dùng dấu chấm thập phân.
