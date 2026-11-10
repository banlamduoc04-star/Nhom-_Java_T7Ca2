# KỊCH BẢN VÀ KẾT QUẢ KIỂM THỬ ĐUA ĐƠN FLASH SALE (CONCURRENCY & OVERSELL PREVENTION)
**Dự án:** OISM  
**Chuyên gia thiết kế:** Trương Quang Vinh & Lê Quốc Khánh  
**Người kiểm thử:** Dung Hoang  
**Sprint:** 3  

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
Bởi vì tất cả các transaction trong toàn hệ thống đều duyệt và khóa SKU theo cùng một thứ tự toàn cục (Total Ordering), đồ thị chờ đợi phụ thuộc chu trình (Wait-For Graph) luôn là đồ thị có hướng không chu trình (DAG) -> **Loại trừ triệt để 100% khả năng xảy ra Deadlock.**

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

## 3. Kịch bản thực nghiệm Flash Sale

### Kịch bản chi tiết:
- **Sản phẩm:** `POLO-SPORT-M`
- **Tồn kho thực tế (OnHand):** 10 sản phẩm
- **Tồn kho đang giữ (Reserved):** 0
- **Tồn kho khả dụng (Available):** 10
- **Tải giả lập:**
  - 25 đơn hàng đồng thời từ sàn Shopee và TikTok Shop.
  - Mỗi đơn hàng mua 1 sản phẩm.
  - Thời gian phát sinh đồng thời (Concurrency Burst).

### Kết quả đo lường:
- **Tổng số request gửi đi:** 25
- **Số đơn tạo và giữ hàng thành công:** 10 đơn (Mã DH-20261104-0001 đến DH-20261104-0010)
- **Số đơn bị từ chối:** 15 đơn (Lỗi: `IV40901 - Không đủ hàng khả dụng`)
- **Tồn kho sau Flash Sale:**
  - `on_hand`: 10
  - `reserved`: 10
  - `available`: 0
- **Số vụ Deadlock ghi nhận:** 0 (Không có bất kỳ deadlock nào)
- **Kết luận:** Hệ thống bảo toàn tính toàn vẹn tồn kho tuyệt đối, không có tình trạng bán âm kho.
