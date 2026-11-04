# TÀI LIỆU THIẾT KẾ CHI TIẾT SPRINT 3
**Dự án:** OISM  
**Nhóm phụ trách:** Backend (M2, M3, M4, M5) & Frontend (M6, M7)  
**Ngày phát hành:** 04/11/2026  

---

## 1. Kiến trúc tổng thể và Tuân thủ Clean Architecture
Hệ thống tuân thủ nghiêm ngặt 4 tầng Clean Architecture, được kiểm soát tự động bởi bộ luật ArchUnit:
1. **Domain:** Chứa các Entity cốt lõi (`SalesOrder`, `OrderItem`, `StockTransfer`, `Stocktake`, `StockBalance`), Value Objects, Enum và Repository Interfaces. Không phụ thuộc bất kỳ tầng nào.
2. **Application:** Chứa các Use Case Services (`OrderService`, `ReservationService`, `StockTransferService`, `StocktakeService`, `ProfitReportService`).
3. **Infrastructure:** Triển khai Repository JPA, cấu hình bảo mật `SecurityConfig`, chuyển ngữ cơ sở dữ liệu `Flyway`.
4. **Web:** Cung cấp RESTful Controller (`OrderController`, `StockTransferController`, `StocktakeController`, `ProfitReportController`, `SimulatorController`, `WebhookController`). Web độc lập với Infrastructure.

---

## 2. Thiết kế Cơ sở dữ liệu (Database Schema & Indexing - Flyway V5)

### 2.1 Bảng `stock_transfer` và `stock_transfer_item`
- `stock_transfer`: Quản lý phiếu điều chuyển liên chi nhánh (`from_branch_id`, `to_branch_id`, `status: DRAFT, DISPATCHED, RECEIVED, CANCELLED`).
- `stock_transfer_item`: Chi tiết từng SKU và số lượng cần chuyển (`transfer_id`, `sku_id`, `quantity`).

### 2.2 Bảng `stocktake` và `stocktake_item`
- `stocktake`: Phiếu kiểm kê kho theo từng chi nhánh (`branch_id`, `status: OPEN, APPROVED, CANCELLED`).
- `stocktake_item`: Mặt hàng kiểm kê (`system_qty`, `counted_qty`, `difference`).

### 2.3 Composite Indexes tối ưu hóa truy vấn đa chi nhánh
```sql
-- V5__sprint3_indexes.sql
CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_created 
ON sales_orders (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_tenant_status 
ON stock_transfers (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_stocktake_tenant_status 
ON stocktakes (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_tx_tenant_sku_created 
ON inventory_transactions (tenant_id, sku_id, created_at DESC);
```

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
  │                │                └─(cancel)────> CANCELLED (Hoàn lại on_hand qua IN_RETURN)
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
