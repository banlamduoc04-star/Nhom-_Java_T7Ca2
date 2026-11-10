# KỊCH BẢN VÀ KẾT QUẢ KIỂM THỬ ĐUA ĐƠN FLASH SALE (CONCURRENCY & OVERSELL PREVENTION)
**Dự án:** OISM  
**Chuyên gia thiết kế:** Trương Quang Vinh & Lê Quốc Khánh  
**Người kiểm thử:** Dung Hoang  
**Sprint:** 3 · **Chạy lại:** 11/11/2026 trên bản Docker (Sprint 5), số liệu bên dưới là kết quả thật  

---

## 1. Bối cảnh bài toán
Trong sự kiện Flash Sale trên sàn Thương mại điện tử (Shopee, TikTok Shop), hàng trăm đơn hàng có thể đổ về hệ thống trong cùng một giây.
Rủi ro chí mạng thường gặp:
1. **Oversell (Bán âm kho):** Kho chỉ còn 10 cái nhưng 20 khách hàng đặt cùng lúc và đều thanh toán thành công -> Cửa hàng bị phạt vi phạm hợp đồng sàn TMĐT vì không giao được hàng.
2. **Deadlock (Tắc nghẽn cơ sở dữ liệu):** Luồng 1 giữ khóa SKU A và chờ SKU B, trong khi Luồng 2 giữ khóa SKU B và chờ SKU A -> Cơ sở dữ liệu bị tê liệt và sinh ngoại lệ `DeadlockLoserDataAccessException`.

---

## 2. Giải pháp kỹ thuật triển khai tại OISM

### 2.1 Thuật toán Khóa có thứ tự (Deadlock-Free Locking Order)
Tại `ReservationService.java`:
Mỗi khi có đơn hàng yêu cầu giữ hàng nhiều dòng sản phẩm, hệ thống bắt buộc sắp xếp danh sách `OrderItem` theo thứ tự tăng dần của khóa ngoại `sku_id`:
```java
List<OrderItem> sorted = items.stream()
        .sorted(Comparator.comparing(OrderItem::getSkuId))
        .toList();
```
Sau đó thực hiện khóa tuần tự từng dòng trong `stock_balance` bằng câu lệnh:
```sql
SELECT * FROM stock_balance 
WHERE tenant_id = ? AND branch_id = ? AND sku_id = ? 
FOR UPDATE;
```
Bởi vì tất cả các transaction trong toàn hệ thống đều duyệt và khóa SKU theo cùng một thứ tự toàn cục (Total Ordering), đồ thị chờ (Wait-For Graph) giữa các giao dịch giữ hàng không thể có chu trình, nên không xảy ra deadlock giữa chúng. Kiểm chứng: `OrderLifecycleIT.concurrentOrdersLockingSkusInOppositeOrderNeverDeadlock` (20 đơn đồng thời, nửa đặt A→B, nửa đặt B→A, cả 20 đều thành công).

### 2.2 Kiểm tra Tồn khả dụng (Strict Available Check)
Sau khi giữ khóa độc quyền trên dòng tồn kho:
```java
if (balance.getAvailable() < item.getQuantity()) {
    throw new BusinessException(ErrorCode.IV40901, "Không đủ hàng khả dụng...");
}
balance.applyReserved(balance.getReserved() + item.getQuantity());
```
Nếu bất kỳ SKU nào trong đơn không đủ số lượng khả dụng (`available = on_hand - reserved`), transaction lập tức ném `BusinessException` và Rollback toàn bộ trạng thái của đơn hàng đó.

---

## 3. Kịch bản thực nghiệm và kết quả thật

Chạy bằng màn/API Simulator (`POST /api/v1/simulator/orders`, gửi N đơn song song qua `OrderService.createAndReserve`) trên bản Docker dựng từ nhánh Sprint 5, lúc 2026-11-10T20:12:48+00:00 (UTC). Mã đơn do hệ thống sinh ngẫu nhiên dạng `DH-yyyyMMdd-xxxx`.

### 3.1. Kịch bản chính theo kế hoạch (NFR-PERF-02): 50 đơn vào SKU còn 1 cái

| Đầu vào | Giá trị |
|---|---|
| SKU | `POLO-SPORT-M-…`, tồn `on_hand` = 1, `reserved` = 0 |
| Tải | 50 đơn đồng thời kênh Shopee, mỗi đơn 1 cái |

| Kết quả | Mong đợi | Thực tế |
|---|---|---|
| Đơn giữ hàng thành công | 1 | **1** (`DH-20261110-8239`) |
| Đơn bị từ chối `IV40901` | 49 | **49** |
| Lỗi khác | 0 | **0** |
| Tồn sau cùng `on_hand` / `reserved` / `available` | 1 / 1 / 0 | **1 / 1 / 0** |
| Thời gian xử lý 50 đơn | — | 159 ms |

### 3.2. Kịch bản phụ: 25 đơn vào SKU còn 10 cái

| Kết quả | Mong đợi | Thực tế |
|---|---|---|
| Đơn giữ hàng thành công | 10 | **10** |
| Đơn bị từ chối `IV40901` | 15 | **15** |
| Tồn sau cùng `on_hand` / `reserved` / `available` | 10 / 10 / 0 | **10 / 10 / 0** |
| Thời gian xử lý 25 đơn | — | 185 ms |

Mã 10 đơn thành công: `DH-20261110-2565`, `DH-20261110-8419`, `DH-20261110-5252`, `DH-20261110-7996`, `DH-20261110-7006`, `DH-20261110-6480`, `DH-20261110-3059`, `DH-20261110-5068`, `DH-20261110-4433`, `DH-20261110-6069`.

### 3.3. Kiểm thử tự động tương ứng (chạy trong CI)

- `FlashSaleIT.flashSale_50Threads_1Stock_OnlyOneSuccess`: 50 luồng `ExecutorService` gọi trực tiếp service, PostgreSQL thật (Testcontainers).
- `FlashSaleIT.simulator_50ConcurrentOrders_1Stock_exactlyOneReserved`: cùng kịch bản qua API Simulator.
- `InventoryIT.concurrentOutNeverOversells`: xuất kho đồng thời không làm âm tồn.

### 3.4. Kết luận

Ở cả hai kịch bản, số đơn thành công đúng bằng số hàng khả dụng, mọi đơn còn lại bị từ chối với `IV40901`, `available` không âm và không có lỗi hệ thống. Đạt NFR-PERF-02.
