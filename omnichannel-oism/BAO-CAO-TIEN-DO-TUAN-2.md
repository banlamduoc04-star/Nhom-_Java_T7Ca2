# BÁO CÁO TIẾN ĐỘ TUẦN 2

* **Đề tài:** Hệ thống Quản lý Bán hàng & Tồn kho Đa kênh (OISM)
* **Sinh viên thực hiện:** Thành viên 2 (Phụ trách Phân hệ Quản lý Hàng hóa - Product Catalog)
* **Tuần thực hiện:** Tuần 2
* **Nhánh Git:** `feature/tuan2-sku-variant`

---

## 1. Mục tiêu nhiệm vụ Tuần 2
Theo phân công công việc của nhóm, nhiệm vụ của Thành viên 2 trong Tuần 2 gồm:
1. **Quản lý biến thể sản phẩm (SKU):** Thiết kế và triển khai logic biến thể hàng hóa gồm kích thước, màu sắc (`attributeValues`), mã vạch (`barcode`), giá vốn (`costPrice`), giá bán lẻ (`retailPrice`) và giá bán sỉ (`wholesalePrice`).
2. **Quản lý thương hiệu (Brand):** Thêm mới thực thể Brand và các chức năng CRUD cơ bản gắn liền với sản phẩm.
3. **Danh mục phân cấp (Category Hierarchy):** Cập nhật danh mục hỗ trợ quan hệ cha - con (`parentId`).
4. **Kiểm tra tính toàn vẹn dữ liệu:** Ràng buộc không trùng lặp mã `skuCode` và `barcode` trong cùng một cửa hàng (`tenant_id`), áp dụng cơ chế xóa mềm (`deleted = true`).
5. **Kiểm thử đơn vị (Unit Testing):** Viết test case cho tầng Service bằng JUnit 5 và Mockito.
6. **Giao diện quản lý (Frontend Admin):** Hoàn thiện trang quản lý sản phẩm, thương hiệu và biến thể SKU bằng Next.js.

---

## 2. Kết quả thực hiện chi tiết

### 2.1. Backend (Spring Boot 3 & Java 21)

#### a. Thực thể (Entities) & DTO:
* **Brand:** `id`, `name`, `description`, `tenant_id`, `deleted`.
* **Category:** Cập nhật liên kết `parent_id` trỏ về Category cha, `tenant_id`, `deleted`.
* **Product:** Cập nhật khóa ngoại `brand_id`, `category_id`, `tenant_id`, `deleted`.
* **Sku:** `skuCode`, `product_id`, `attributeValues`, `barcode`, `costPrice`, `sellingPrice`, `retailPrice`, `wholesalePrice`, `tenant_id`, `deleted`.
* **DTO chuẩn:** `BrandRequest`, `BrandResponse`, `CategoryRequest`, `CategoryResponse`, `ProductRequest`, `ProductResponse`, `SkuRequest`, `SkuResponse`, `PageResponse`.

#### b. Danh sách REST API đã hoàn thành:
| Phân hệ | Phương thức | Endpoint | Chức năng |
| :--- | :--- | :--- | :--- |
| **Category** | `POST` | `/api/v1/categories` | Tạo mới danh mục (có hỗ trợ `parentId`) |
| | `GET` | `/api/v1/categories` | Lấy danh sách toàn bộ danh mục |
| | `GET` | `/api/v1/categories/{id}` | Lấy chi tiết danh mục theo ID |
| | `PUT` | `/api/v1/categories/{id}` | Cập nhật thông tin danh mục |
| | `DELETE` | `/api/v1/categories/{id}` | Xóa mềm danh mục (`deleted = true`) |
| **Brand** | `POST` | `/api/v1/brands` | Tạo thương hiệu mới |
| | `GET` | `/api/v1/brands` | Lấy danh sách thương hiệu |
| | `GET` | `/api/v1/brands/{id}` | Lấy chi tiết thương hiệu |
| | `PUT` | `/api/v1/brands/{id}` | Cập nhật thương hiệu |
| | `DELETE` | `/api/v1/brands/{id}` | Xóa mềm thương hiệu |
| **Product** | `POST` | `/api/v1/products` | Tạo sản phẩm (kèm `categoryId`, `brandId`) |
| | `GET` | `/api/v1/products` | Lấy danh sách sản phẩm |
| | `GET` | `/api/v1/products/search` | Tìm kiếm theo tên, lọc danh mục, phân trang |
| | `GET` | `/api/v1/products/{id}` | Lấy thông tin chi tiết sản phẩm |
| | `PUT` | `/api/v1/products/{id}` | Cập nhật sản phẩm |
| | `DELETE` | `/api/v1/products/{id}` | Xóa mềm sản phẩm |
| **SKU** | `POST` | `/api/v1/products/{productId}/skus` | Thêm biến thể SKU cho sản phẩm |
| | `GET` | `/api/v1/products/{productId}/skus` | Lấy danh sách biến thể của sản phẩm |
| | `GET` | `/api/v1/skus/barcode/{barcode}` | Tra cứu biến thể bằng mã vạch (phục vụ POS) |
| | `PUT` | `/api/v1/skus/{skuId}` | Cập nhật biến thể SKU |
| | `DELETE` | `/api/v1/skus/{skuId}` | Xóa mềm biến thể SKU |

### 2.2. Kết quả Kiểm thử đơn vị (Unit Test)
Toàn bộ mã kiểm thử được viết bằng JUnit 5 và Mockito, tuân thủ đúng chuẩn quy ước đặt tên `camelCase`:

* `BrandServiceImplTest.java` (3 ca kiểm thử):
  * `testCreateBrandSuccess`: Tạo thương hiệu thành công.
  * `testGetBrandByIdNotFound`: Ném `ResourceNotFoundException` khi không tìm thấy ID.
  * `testDeleteBrandSuccess`: Xóa mềm chuyển cờ `deleted = true`.
* `CategoryServiceImplTest.java` (3 ca kiểm thử):
  * `testCreateCategorySuccess`: Tạo danh mục thành công.
  * `testGetCategoryByIdNotFound`: Bắt lỗi khi không tìm thấy ID.
  * `testDeleteCategorySuccess`: Xóa mềm danh mục.
* `ProductServiceImplTest.java` (4 ca kiểm thử):
  * `testCreateProductSuccess`: Tạo sản phẩm thành công kèm danh mục và thương hiệu.
  * `testCreateProductCategoryNotFound`: Bắt lỗi khi danh mục không tồn tại.
  * `testGetProductByIdSuccess`: Lấy chi tiết sản phẩm.
  * `testDeleteProductSuccess`: Xóa mềm sản phẩm.
* `SkuServiceImplTest.java` (3 ca kiểm thử):
  * `testAddSkuSuccess`: Thêm biến thể SKU thành công khi mã SKU và mã vạch hợp lệ.
  * `testAddSkuDuplicateSkuCode`: Ném `DuplicateResourceException` khi trùng mã SKU trong cùng một cửa hàng.
  * `testGetSkuByBarcodeSuccess`: Tra cứu nhanh SKU bằng mã vạch.

> **Tổng kết kết quả chạy test (`mvn test`):**
> * **Tests run: 14**
> * **Failures: 0, Errors: 0, Skipped: 1** (`BackendApplicationTests` bật khi kết nối DB thật)
> * **Trạng thái: BUILD SUCCESS (100% Passed)**

### 2.3. Frontend Admin (Next.js)
* Cập nhật định nghĩa kiểu dữ liệu trong `frontend-admin/types/catalog.ts` bổ sung `Brand`, `Sku` (giá sỉ, giá lẻ), `Category` (`parentId`).
* Xây dựng tầng gọi API `frontend-admin/services/catalogService.ts` cho Thương hiệu, Sản phẩm và Biến thể SKU.
* Xây dựng trang giao diện `frontend-admin/app/products/page.tsx`:
  * Xem danh sách sản phẩm dạng bảng kèm danh mục, thương hiệu.
  * Modal thêm mới sản phẩm và lọc theo danh mục.
  * Xem danh sách biến thể SKU của từng sản phẩm.
  * Modal thêm mới biến thể SKU (mã SKU, thuộc tính, barcode, giá vốn, giá bán lẻ, giá bán sỉ).

---

## 3. Đánh giá mã nguồn
* **Phong cách code:** Rõ ràng, dễ hiểu, phù hợp với kiến thức môn học Java Web.
* **Quy ước đặt tên:** 100% tuân thủ `camelCase` cho biến, hàm và ca kiểm thử.
* **Chú thích (Comments):** Dùng cụm từ ngắn gọn, tự nhiên, chỉ rõ mục đích của hàm (không sử dụng văn phong máy móc hay chú thích thừa).
* **Đóng gói mã nguồn:** Sẵn sàng trên nhánh `feature/tuan2-sku-variant` để nhóm trưởng tích hợp.
