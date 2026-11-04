# Kế hoạch dự án OISM – 5 Sprint, 7 thành viên

@Hoang\_Xiaong

## 1. Tổng quan và công nghệ

OISM là hệ thống quản lý bán hàng và tồn kho đa kênh theo mô hình SaaS nhiều cửa hàng (multi-tenant): backend Java Spring Boot, frontend React, cơ sở dữ liệu PostgreSQL thiết kế mới hoàn toàn. Dự án chia thành 5 sprint, 7 thành viên (M1–M7), mỗi sprint kết thúc bằng một bản demo chạy được.

| Lớp | Công nghệ |
| --- | --- |
| Backend | Java 17, Spring Boot 3 (MVC + REST), Spring Security, Spring Data JPA/Hibernate |
| Cơ sở dữ liệu | PostgreSQL, Flyway migration |
| Xác thực | JWT (access token 60 phút + refresh token), mật khẩu BCrypt |
| Cô lập tenant | Cột `tenant_id` + Hibernate `@Filter` bật theo từng request |
| Chống bán vượt | Khóa pessimistic `@Lock(PESSIMISTIC_WRITE)` trong `@Transactional` |
| Thông báo realtime | WebSocket (STOMP) |
| Tác vụ nền | `@Scheduled` |
| Tài liệu API | springdoc-openapi (Swagger UI) + Postman collection |
| Frontend | React + Vite + TypeScript; POS dạng PWA |
| Kiểm thử | JUnit 5, Mockito, Testcontainers; test đồng thời bằng `ExecutorService` |
| Triển khai | Docker, docker-compose, GitHub Actions |

**Chuẩn chung của đề Spring Boot, áp dụng cho toàn bộ API:** đăng nhập JWT theo role, JSON lỗi `{errorCode, message}` với mã dạng `PR40001`, Swagger có nhập token và hiển thị rule validation, bộ test Postman, mã nguồn phải biên dịch được (lỗi biên dịch = 0 điểm).

## 2. Yêu cầu chức năng chi tiết

Hệ thống có 3 vai trò: **Owner** (toàn quyền trong cửa hàng), **Staff** (kho và đơn hàng), **Cashier** (chỉ POS). Mỗi yêu cầu dưới đây kèm quy tắc nghiệp vụ, dùng luôn làm tiêu chí nghiệm thu khi test. Mục ghi "(bổ sung)" là yêu cầu đề gốc chưa nêu nhưng cần để hệ thống chạy được.

### 2.1 AUTH – Tenant, xác thực, phân quyền

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-AUTH-01 | Đăng ký cửa hàng (tenant) | Tạo tenant mới + tài khoản Owner đầu tiên + chi nhánh mặc định trong một giao dịch. Email Owner duy nhất toàn hệ thống. Mọi dữ liệu tạo sau đó gắn `tenant_id` của cửa hàng | Khách chưa đăng nhập |
| FR-AUTH-02 | Đăng nhập, làm mới token, đăng xuất | Đăng nhập bằng email hoặc số điện thoại + mật khẩu. Trả access token (60 phút) chứa `tenantId`, `userId`, `role` và refresh token. Sai thông tin → 401 `AU40101`. Tài khoản bị khóa → 403 `AU40302`. Đăng xuất thu hồi refresh token | Tất cả |
| FR-AUTH-03 | Phân quyền theo vai trò (RBAC) | Owner: mọi chức năng. Staff: xem sản phẩm, thao tác kho, xử lý đơn, xem báo cáo tồn. Cashier: POS và tra cứu sản phẩm. Gọi API sai quyền → 403 `AU40301`; Admin ẩn menu không được phép | Hệ thống |
| FR-AUTH-04 | Quản lý chi nhánh/kho | Tạo, sửa, ẩn/hiện. Loại: Cửa hàng hoặc Kho. Tên duy nhất trong tenant. Không ẩn được chi nhánh còn tồn kho hoặc còn đơn chưa kết thúc | Owner |
| FR-AUTH-05 (bổ sung) | Quản lý tài khoản nhân viên | Owner tạo tài khoản Staff/Cashier, gán chi nhánh, khóa/mở khóa, đặt lại mật khẩu. Cashier chỉ bán được tại chi nhánh được gán | Owner |

### 2.2 PROD – Sản phẩm và biến thể

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-PROD-01 | Danh mục nhiều cấp, thương hiệu | Danh mục có danh mục cha. Tên duy nhất trong cùng cấp. Không xóa được danh mục/thương hiệu còn sản phẩm (409 `PR40902`) | Owner; Staff xem |
| FR-PROD-02 | Sản phẩm đơn và sản phẩm có biến thể | Mỗi biến thể (màu, size…) là một SKU; sản phẩm đơn có đúng 1 SKU. Mã SKU duy nhất trong tenant (409 `PR40901`). Tên sản phẩm khớp `^[A-Z][a-zA-Z0-9\s]{2,50}$`. SKU đã có giao dịch kho thì chỉ được ngưng bán, không xóa | Owner |
| FR-PROD-03 | Mã vạch | Tự sinh EAN-13 (có kiểm tra số check digit) hoặc nhập tay Code128. Mã vạch duy nhất trong tenant. In tem mã vạch từ Admin | Owner, Staff |
| FR-PROD-04 | Giá bán lẻ, giá sỉ | Giá > 0, tách biệt với giá vốn. Đổi giá không ảnh hưởng đơn đã tạo (đơn lưu `unit_price` tại thời điểm bán) | Owner |
| FR-PROD-05 (bổ sung) | Tìm kiếm, lọc, phân trang | Tìm theo tên/SKU (gần đúng) và mã vạch (khớp chính xác); lọc danh mục, thương hiệu, trạng thái; mặc định 20 dòng/trang | Tất cả |

### 2.3 INV – Sổ cái và nghiệp vụ kho

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-INV-01 | Sổ cái tồn kho | Mọi biến động (Nhập, Bán, Trả, Kiểm kê, Chuyển đi, Chuyển đến) ghi 1 dòng: `tenant_id`, `branch_id`, `sku_id`, loại IN/OUT, lý do, số lượng, `balance_after`, chứng từ gốc, người tạo, thời điểm. Không có API sửa/xóa; sửa sai bằng bút toán bù | Hệ thống ghi; Owner, Staff xem |
| FR-INV-02 | Phiếu nhập kho từ nhà cung cấp | Trạng thái Nháp → Đã xác nhận. Chỉ khi xác nhận mới ghi sổ cái IN, tăng `on_hand`, tính lại giá vốn. Phiếu đã xác nhận không sửa được (409 `IV40902`) | Owner, Staff |
| FR-INV-03 | Chuyển kho 2 bước | Bước 1 (kho A xuất): ghi OUT ở A, phiếu sang Đang chuyển. Bước 2 (kho B xác nhận): ghi IN ở B, phiếu sang Đã nhận. Không chuyển quá `available` của A. Giá vốn không đổi vì tính theo SKU trên toàn tenant | Owner, Staff |
| FR-INV-04 | Kiểm kê | Tạo phiên → nhập số đếm thực tế → hệ thống tính chênh lệch so với số dư lúc chốt → Owner duyệt → ghi bút toán Điều chỉnh IN/OUT. Phiên đã duyệt không sửa được | Staff tạo, Owner duyệt |
| FR-INV-05 (bổ sung) | Nhà cung cấp | Thêm, sửa, ngưng dùng nhà cung cấp cho phiếu nhập | Owner, Staff |

### 2.4 COST – Giá vốn

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-COST-01 | Tính giá vốn bình quân gia quyền khi xác nhận phiếu nhập | Tính theo SKU trên toàn tenant (công thức và ví dụ ở mục 4). Tồn cũ bằng 0 thì giá vốn mới = giá nhập. Làm tròn 2 chữ số thập phân | Hệ thống |
| FR-COST-02 | Chụp giá vốn vào đơn | Khi đơn sang Confirmed hoặc thanh toán tại POS, lưu giá vốn hiện tại vào `order_item.cost_price`. Giá vốn thay đổi sau đó không làm đổi báo cáo của đơn cũ | Hệ thống |

### 2.5 ORD – Trung tâm đơn hàng

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-ORD-01 | Tiếp nhận đơn đa kênh | Đơn từ POS, Admin tạo tay, webhook Shopee/TikTok Shop đều chuẩn hóa về một mô hình đơn chung (kênh, mã đơn ngoài, khách hàng, dòng hàng). Cùng kênh + cùng mã đơn ngoài gửi lại thì không tạo đơn mới (chống trùng) | Hệ thống, Owner, Staff |
| FR-ORD-02 | Chuyển trạng thái đơn | Chỉ cho phép các bước trong bảng chuyển trạng thái ở mục 4; sai bước → 409 `OD40901`. Completed và Cancelled là trạng thái cuối. Mỗi lần chuyển ghi lịch sử: từ, sang, người làm, lý do | Hệ thống |
| FR-ORD-03 | Xử lý đơn | Danh sách đơn lọc theo kênh, trạng thái, chi nhánh, ngày; xem chi tiết và lịch sử; duyệt, hoàn tất, hủy (bắt buộc nhập lý do); in phiếu giao hàng | Owner, Staff |

### 2.6 RSE – Chống bán vượt tồn kho

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-RSE-01 | Tính hàng khả dụng | `available = on_hand − reserved` theo từng SKU × chi nhánh, lưu trong `stock_balance`; `available` không bao giờ âm | Hệ thống |
| FR-RSE-02 | Giữ hàng | Đơn sang Reserved thì `reserved` tăng theo số lượng. Khóa dòng tồn trước khi kiểm tra. Một dòng hàng thiếu thì từ chối cả đơn (409 `IV40901`). Mỗi đơn giữ có hạn `reserved_until` (mặc định 30 phút, Owner cấu hình được) | Hệ thống |
| FR-RSE-03 | Trừ và nhả hàng | Confirmed: giảm `on_hand` và `reserved`, ghi sổ cái Bán (OUT). Cancelled từ Reserved: giảm `reserved`. Cancelled từ Confirmed: tăng `on_hand`, ghi sổ cái Trả (IN). Tất cả nằm trong một giao dịch | Hệ thống |

### 2.7 POS – Bán hàng tại quầy

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-POS-01 | Giao diện bán nhanh | Tìm theo tên/SKU/mã vạch; quét mã vạch + Enter thêm ngay vào giỏ (quét lại thì tăng số lượng); phím tắt F2 tìm, F9 thanh toán, Esc hủy giỏ; cài đặt được như ứng dụng (PWA) | Cashier |
| FR-POS-02 | Ràng buộc tồn khi bán | Kiểm tra `available` khi thêm vào giỏ và khi sửa số lượng; vượt thì chặn và báo số còn lại, để không ăn vào hàng đang giữ cho đơn online | Cashier |
| FR-POS-03 | Thanh toán và in hóa đơn | Tiền mặt (nhập tiền khách đưa, tính tiền thối) hoặc QR chuyển khoản (hiển thị mã QR, thu ngân xác nhận đã nhận). Thanh toán xong tự mở hộp thoại in hóa đơn khổ 80 mm | Cashier |
| FR-POS-04 | Thanh toán một giao dịch | Tạo đơn → Giữ → Xác nhận → Trừ kho → Ghi thanh toán trong một giao dịch CSDL; lỗi bất kỳ bước nào thì hoàn tác toàn bộ | Hệ thống |

### 2.8 REP – Báo cáo và cảnh báo

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-REP-01 | Doanh thu và lợi nhuận gộp | Doanh thu thuần, tổng giá vốn (từ `order_item.cost_price`), lợi nhuận gộp = doanh thu − giá vốn. Chỉ tính đơn Confirmed và Completed. Lọc theo khoảng ngày, chi nhánh, kênh, SKU; xuất CSV | Owner |
| FR-REP-02 | Tồn kho và tốc độ bán | Giá trị tồn = `on_hand` × giá vốn; top 10 bán chạy theo số lượng và doanh thu; hàng tồn chậm = còn tồn nhưng không bán trong N ngày (mặc định 30) | Owner, Staff |
| FR-REP-03 | Cảnh báo tồn thấp | Khi `available` ≤ ngưỡng đặt hàng lại của SKU thì tạo cảnh báo và đẩy thông báo lên dashboard. Mỗi SKU × chi nhánh chỉ có một cảnh báo mở; tự đóng khi nhập thêm vượt ngưỡng | Owner, Staff |

### 2.9 SIM – Mô phỏng sàn, thời gian thực, tác vụ nền

| Mã | Chức năng | Quy tắc nghiệp vụ / tiêu chí nghiệm thu | Vai trò |
| --- | --- | --- | --- |
| FR-SIM-01 | Webhook Simulator | Endpoint nhận webhook theo từng sàn, xác thực bằng API key của tenant; payload Shopee/TikTok Shop được ánh xạ về mô hình đơn chung. Màn hình mô phỏng cho phép chọn sàn, SKU, số đơn và gửi song song để thử tải | Owner (người test) |
| FR-SIM-02 | Thông báo thời gian thực | WebSocket đẩy đơn mới và cảnh báo tồn tới Admin/POS cùng tenant (kênh riêng theo `tenant_id`); hiển thị popup + âm thanh, không cần tải lại trang | Hệ thống |
| FR-SIM-03 | Tác vụ nền | Mỗi phút quét và hủy đơn Reserved quá `reserved_until`, nhả hàng giữ; mỗi đêm tổng hợp báo cáo ngày vào `daily_report` | Hệ thống |

## 3. Yêu cầu phi chức năng

Mỗi yêu cầu có chỉ số đo và cách kiểm tra cụ thể, được chứng minh trong tài liệu kiểm thử ở Sprint 5.

| Mã | Yêu cầu | Chỉ số đạt | Cách kiểm tra |
| --- | --- | --- | --- |
| NFR-PERF-01 | Thời gian phản hồi | Tìm SKU/quét mã vạch p95 < 200 ms; tạo đơn/ghi dữ liệu < 500 ms; báo cáo < 2 s với dưới 100.000 bản ghi | JMeter trên bản Docker, dữ liệu mẫu lớn |
| NFR-PERF-02 | Đồng thời (Flash Sale) | 50 đơn đồng thời vào 1 SKU còn 1 cái: đúng 1 đơn thành công, 49 đơn bị từ chối `IV40901`, `available` cuối = 0, không âm | Test tích hợp dùng `ExecutorService` + Testcontainers PostgreSQL, chạy trong CI |
| NFR-TENANT-01 | Cô lập dữ liệu | Mọi bảng nghiệp vụ có `tenant_id`; người dùng tenant A gọi dữ liệu tenant B nhận 404 | Test tự động với 2 tenant cho mọi API đọc/ghi |
| NFR-TENANT-02 | Khả năng mở rộng | Index `(tenant_id, created_at)` và `(tenant_id, sku_id)` trên sổ cái và đơn hàng; truy vấn sổ cái vẫn < 500 ms với 1 triệu dòng | `EXPLAIN ANALYZE` xác nhận dùng index |
| NFR-SEC-01 | Mã hóa và xác thực | Mật khẩu BCrypt; HTTPS; access token 60 phút; refresh token 7 ngày, xoay vòng mỗi lần dùng, lưu dạng băm | Test bảo mật API, kiểm tra DB không có mật khẩu thô |
| NFR-SEC-02 | Toàn vẹn giao dịch (ACID) | Chuỗi Tạo đơn → Giữ → Trừ lỗi giữa chừng không để lại dữ liệu dở dang | Test giả lập lỗi ở từng bước, kiểm tra đã hoàn tác |
| NFR-SEC-03 | Sổ cái chỉ ghi thêm | Không có API sửa/xóa; trigger PostgreSQL từ chối mọi `UPDATE`/`DELETE` trên `inventory_transaction` | Test gọi `UPDATE` trực tiếp phải lỗi |
| NFR-USA-01 | Tính sẵn sàng | 99,5% trong giờ hoạt động 7:00–22:00 | Healthcheck Docker + tự khởi động lại container |
| NFR-USA-02 | Dễ dùng POS | Thanh toán tối đa 3 thao tác hoặc Quét mã + Enter; chạy tốt trên máy tính, tablet, điện thoại | Test E2E Playwright ở 3 kích thước màn hình |
| NFR-MAINT-01 | Clean Architecture | Lớp `domain` không phụ thuộc `web`/`infrastructure`; controller không gọi repository trực tiếp | Test ArchUnit trong CI |
| NFR-MAINT-02 | Phủ test | ≥ 80% cho sổ cái, giữ hàng, giá vốn | JaCoCo trong CI, dưới ngưỡng thì pipeline đỏ |

## 4. Phân tích thiết kế

Phần thiết kế làm ở Sprint 1 và chỉnh dần ở các sprint sau. Sơ đồ (UML, ERD, sequence) sẽ vẽ sau.

**Sản phẩm thiết kế**

| Sản phẩm | Nội dung | Phụ trách | Sprint |
| --- | --- | --- | --- |
| URS + RTM | Yêu cầu người dùng; ánh xạ yêu cầu → thiết kế → mã → test | M1 | 1, cập nhật mỗi sprint |
| Kiến trúc | Phân lớp, cô lập tenant, bảo mật, cấu trúc package | M2 | 1 |
| Thiết kế dữ liệu | Bảng, khóa, index `(tenant_id, created_at)` và `(tenant_id, sku_id)`, script Flyway | M4 | 1–2 |
| Thiết kế lớp/domain | Entity, service, quy tắc nghiệp vụ | M3, M4, M5 | 1–3 |
| Thiết kế luồng xử lý | Giữ hàng, thanh toán nhanh, nhập kho + giá vốn, state machine đơn | M5, M4 | 1–3 |
| Hợp đồng API | Endpoint, request/response, mã lỗi, phân quyền (OpenAPI) | M2 + từng module | 1, cập nhật liên tục |
| Thiết kế giao diện | Wireframe Admin và POS, luồng màn hình, component dùng chung | M6, M7 | 1–2 |
| Kế hoạch kiểm thử | Test case theo yêu cầu, kịch bản Flash Sale, dữ liệu mẫu | M1 | 2 |
| Kế hoạch triển khai | Dockerfile, compose, CI/CD, biến môi trường | M2 | 2, 5 |

**Quyết định kiến trúc**

1. Clean Architecture trong Spring Boot: `domain` (entity, quy tắc), `application` (use case/service), `infrastructure` (JPA, security, scheduler, websocket), `web` (controller, DTO, exception handler).
2. Sổ cái tồn kho: không có cột tồn nào được sửa trực tiếp. `on_hand` và `reserved` nằm trong bảng `stock_balance`, chỉ được cập nhật cùng giao dịch với dòng sổ cái; sửa sai bằng bút toán bù.
3. Khóa pessimistic trên `stock_balance` cho mọi thao tác giữ, trừ, nhả hàng.
4. Mã lỗi thống nhất `<TIỀN TỐ><HTTP><SỐ>` theo mẫu của giáo viên: PR (sản phẩm), AU (xác thực), IV (tồn kho), OD (đơn hàng), RP (báo cáo); ví dụ `IV40901` = không đủ hàng khả dụng.
5. Quy ước API: tiền tố `/api/v1`, phân trang `page/size`, phân quyền bằng `@PreAuthorize`.
6. Quy tắc làm việc: nhánh `main`, `develop`, `feature/*`; mỗi pull request cần 1 người duyệt và CI xanh (biên dịch + test) mới được merge.

### Mô hình dữ liệu

Cơ sở dữ liệu gồm 24 bảng, chia theo module. Mọi bảng nghiệp vụ có `id` (UUID), `tenant_id`, `created_at`, `updated_at` (trừ sổ cái chỉ có `created_at`); bảng dưới chỉ liệt kê cột riêng. ERD sẽ vẽ từ bảng này.

| Module | Bảng | Cột chính | Ràng buộc / ghi chú |
| --- | --- | --- | --- |
| AUTH | `tenant` | `name`, `status`, `reservation_minutes` | Không có `tenant_id` |
| AUTH | `branch` | `name`, `type` (STORE/WAREHOUSE), `address`, `active` | Duy nhất `(tenant_id, name)` |
| AUTH | `app_user` | `email`, `phone`, `password_hash`, `full_name`, `role` (OWNER/STAFF/CASHIER), `branch_id`, `active` | Email duy nhất toàn hệ thống |
| AUTH | `refresh_token` | `user_id`, `token_hash`, `expires_at`, `revoked` |  |
| AUTH | `channel_credential` | `channel` (SHOPEE/TIKTOK), `api_key_hash`, `active` | Xác thực webhook |
| PROD | `category` | `parent_id`, `name` | Duy nhất `(tenant_id, parent_id, name)` |
| PROD | `brand` | `name` | Duy nhất `(tenant_id, name)` |
| PROD | `product` | `category_id`, `brand_id`, `name`, `description`, `has_variants`, `status` |  |
| PROD | `sku` | `product_id`, `code`, `attributes` (JSONB: màu, size), `barcode`, `retail_price`, `wholesale_price`, `avg_cost`, `reorder_point`, `status` | Duy nhất `(tenant_id, code)` và `(tenant_id, barcode)` |
| INV | `stock_balance` | `branch_id`, `sku_id`, `on_hand`, `reserved` | Duy nhất `(tenant_id, branch_id, sku_id)`; `CHECK (on_hand >= reserved AND reserved >= 0)`; khóa pessimistic |
| INV | `inventory_transaction` | `branch_id`, `sku_id`, `direction` (IN/OUT), `reason` (PURCHASE/SALE/RETURN/ADJUST/TRANSFER\_OUT/TRANSFER\_IN), `quantity`, `balance_after`, `unit_cost`, `ref_type`, `ref_id`, `created_by` | Chỉ ghi thêm (trigger); index `(tenant_id, created_at)`, `(tenant_id, sku_id)` |
| INV | `supplier` | `name`, `phone`, `address`, `active` |  |
| INV | `purchase_receipt` | `branch_id`, `supplier_id`, `code`, `status` (DRAFT/CONFIRMED), `confirmed_at`, `confirmed_by` |  |
| INV | `purchase_receipt_item` | `receipt_id`, `sku_id`, `quantity`, `unit_price` | `quantity > 0`, `unit_price >= 0` |
| INV | `stock_transfer` | `from_branch_id`, `to_branch_id`, `code`, `status` (DRAFT/IN\_TRANSIT/RECEIVED/CANCELLED) | Hai chi nhánh khác nhau |
| INV | `stock_transfer_item` | `transfer_id`, `sku_id`, `quantity` |  |
| INV | `stocktake` | `branch_id`, `code`, `status` (OPEN/APPROVED), `approved_by` |  |
| INV | `stocktake_item` | `stocktake_id`, `sku_id`, `system_qty`, `counted_qty`, `difference` |  |
| ORD | `orders` | `branch_id`, `channel` (POS/MANUAL/SHOPEE/TIKTOK), `external_order_id`, `status`, `customer_name`, `customer_phone`, `shipping_address`, `total_amount`, `reserved_until`, `created_by` | Duy nhất `(tenant_id, channel, external_order_id)`; index `(tenant_id, created_at)` |
| ORD | `order_item` | `order_id`, `sku_id`, `quantity`, `unit_price`, `cost_price` | `cost_price` ghi khi Confirmed |
| ORD | `order_status_history` | `order_id`, `from_status`, `to_status`, `changed_by`, `reason` |  |
| ORD | `payment` | `order_id`, `method` (CASH/QR), `amount`, `cash_received`, `change_amount` |  |
| REP | `stock_alert` | `branch_id`, `sku_id`, `available`, `threshold`, `status` (OPEN/CLOSED) | Mỗi `(branch_id, sku_id)` tối đa 1 cảnh báo OPEN |
| REP | `daily_report` | `branch_id`, `report_date`, `channel`, `order_count`, `revenue`, `cogs`, `gross_profit` | Duy nhất `(tenant_id, branch_id, report_date, channel)` |

### Quy tắc nghiệp vụ lõi

**Chuyển trạng thái đơn hàng.** Chỉ 7 bước dưới đây hợp lệ; mọi bước khác trả 409 `OD40901`.

| Từ | Sang | Khi nào | Tác động tồn kho |
| --- | --- | --- | --- |
| (mới) | Draft | Admin tạo tay | Không đổi |
| (mới) hoặc Draft | Reserved | Webhook sàn đến, hoặc Staff bấm Giữ hàng | `reserved` + SL; đặt `reserved_until` |
| Draft | Cancelled | Người dùng hủy | Không đổi |
| Reserved | Confirmed | Staff duyệt, hoặc POS thanh toán | `on_hand` − SL, `reserved` − SL; sổ cái OUT/SALE; chụp `cost_price` |
| Reserved | Cancelled | Người dùng hủy hoặc quá hạn giữ | `reserved` − SL |
| Confirmed | Completed | Giao hàng thành công (POS: ngay sau thanh toán) | Không đổi |
| Confirmed | Cancelled | Khách hủy/hoàn trước khi giao | `on_hand` + SL; sổ cái IN/RETURN |

**Sổ cái tồn kho.** Mọi thao tác kho đi qua một hàm duy nhất `InventoryLedgerService.post(...)`. Hàm này khóa dòng `stock_balance`, cập nhật `on_hand`, ghi một dòng `inventory_transaction` với `balance_after` bằng `on_hand` mới, tất cả trong cùng giao dịch. Không nơi nào khác được sửa `on_hand`.

**Giá vốn bình quân gia quyền** (tính khi xác nhận phiếu nhập, theo SKU trên toàn tenant):

```latex
\text{Giá vốn mới} = \frac{\text{Tồn cũ} \times \text{Giá vốn cũ} + \text{SL nhập} \times \text{Giá nhập}}{\text{Tồn cũ} + \text{SL nhập}}
```

Ví dụ: đang tồn 10 cái, giá vốn 100.000 đ; nhập thêm 30 cái giá 120.000 đ → (10 × 100.000 + 30 × 120.000) ÷ 40 = 115.000 đ. "Tồn cũ" là tổng `on_hand` của SKU trên mọi chi nhánh.

**Luồng giữ hàng (chống bán vượt)**

1. Mở giao dịch.
2. Khóa các dòng `stock_balance` của đơn bằng `SELECT … FOR UPDATE`, theo thứ tự tăng dần của `sku_id` để tránh deadlock.
3. Với từng dòng hàng, kiểm tra `on_hand − reserved ≥ SL`.
4. Thiếu bất kỳ dòng nào: hoàn tác, trả 409 `IV40901` kèm SKU và số còn lại.
5. Đủ hàng: tăng `reserved`, đổi trạng thái đơn, ghi lịch sử, đặt `reserved_until`.
6. Commit, sau đó phát sự kiện WebSocket "đơn mới".

**Thanh toán tại POS** chạy các bước 1–5 rồi, vẫn trong cùng giao dịch, chuyển đơn Reserved → Confirmed → Completed, trừ kho qua sổ cái, chụp giá vốn, ghi `payment`. Một lỗi bất kỳ làm hoàn tác toàn bộ.

**Cô lập tenant.** Filter bảo mật đọc `tenantId` từ JWT vào `TenantContext`; mỗi request bật Hibernate `@Filter("tenantFilter")`; khi lưu bản ghi mới, `tenant_id` được gán tự động, không lấy từ request body. Webhook lấy `tenantId` từ API key.

### Danh sách API

Tất cả endpoint có tiền tố `/api/v1`, nhận và trả JSON, yêu cầu header `Authorization: Bearer <token>` trừ khi ghi "Công khai". Danh sách hỗ trợ `page`, `size`, `sort`.

| Module | Phương thức và endpoint | Mô tả | Vai trò |
| --- | --- | --- | --- |
| AUTH | `POST /auth/register` | Đăng ký tenant + Owner | Công khai |
| AUTH | `POST /auth/login` | Đăng nhập, trả access + refresh token | Công khai |
| AUTH | `POST /auth/refresh` | Cấp lại access token | Công khai (cần refresh token) |
| AUTH | `POST /auth/logout` | Thu hồi refresh token | Tất cả |
| AUTH | `GET /me` | Thông tin người đang đăng nhập | Tất cả |
| AUTH | `GET, POST /users`; `PUT /users/{id}`; `PATCH /users/{id}/status` | Quản lý nhân viên | Owner |
| AUTH | `GET, POST /branches`; `PUT /branches/{id}`; `PATCH /branches/{id}/status` | Quản lý chi nhánh | Owner (GET: tất cả) |
| PROD | `GET, POST /categories`; `PUT, DELETE /categories/{id}` | Danh mục | Owner (GET: tất cả) |
| PROD | `GET, POST /brands`; `PUT, DELETE /brands/{id}` | Thương hiệu | Owner (GET: tất cả) |
| PROD | `GET /products?keyword=&categoryId=&brandId=&status=` | Tìm kiếm, lọc, phân trang | Tất cả |
| PROD | `GET /products/{id}` | Chi tiết sản phẩm kèm SKU | Tất cả |
| PROD | `POST /products`; `PUT /products/{id}`; `PATCH /products/{id}/status` | Tạo/sửa/ngưng bán sản phẩm | Owner |
| PROD | `POST /products/{id}/skus`; `PUT /skus/{id}`; `PUT /skus/{id}/prices` | Quản lý SKU và giá | Owner |
| PROD | `POST /skus/{id}/barcode` | Sinh mã EAN-13 | Owner, Staff |
| PROD | `GET /skus/lookup?barcode=` | Tra SKU theo mã vạch (POS) kèm `available` | Tất cả |
| INV | `GET /stock?branchId=&skuId=` | Xem `on_hand`, `reserved`, `available` | Tất cả |
| INV | `GET /inventory-transactions?branchId=&skuId=&from=&to=` | Xem sổ cái | Owner, Staff |
| INV | `GET, POST /suppliers`; `PUT /suppliers/{id}` | Nhà cung cấp | Owner, Staff |
| INV | `GET, POST /purchase-receipts`; `PUT /purchase-receipts/{id}`; `POST /purchase-receipts/{id}/confirm` | Phiếu nhập, xác nhận | Owner, Staff |
| INV | `GET, POST /transfers`; `POST /transfers/{id}/dispatch`; `POST /transfers/{id}/receive` | Chuyển kho 2 bước | Owner, Staff |
| INV | `GET, POST /stocktakes`; `PUT /stocktakes/{id}/counts`; `POST /stocktakes/{id}/approve` | Kiểm kê | Staff (duyệt: Owner) |
| ORD | `GET /orders?status=&channel=&branchId=&from=&to=`; `GET /orders/{id}` | Danh sách, chi tiết, lịch sử | Owner, Staff |
| ORD | `POST /orders` | Tạo đơn tay (Draft) | Owner, Staff |
| ORD | `POST /orders/{id}/reserve`, `/confirm`, `/complete`, `/cancel` | Chuyển trạng thái | Owner, Staff |
| ORD | `GET /orders/{id}/shipping-slip` | Dữ liệu in phiếu giao | Owner, Staff |
| POS | `POST /pos/checkout` | Tạo đơn + thanh toán một giao dịch | Cashier, Owner |
| POS | `GET /pos/orders/{id}/receipt` | Dữ liệu in hóa đơn | Cashier, Owner |
| SIM | `POST /webhooks/{channel}` | Nhận đơn từ sàn (header `X-Api-Key`) | Hệ thống ngoài |
| SIM | `POST /simulator/orders` | Sinh N đơn giả và gửi song song | Owner |
| SIM | WebSocket `/ws`, kênh `/topic/tenants/{tenantId}/orders` và `/alerts` | Thông báo thời gian thực | Tất cả cùng tenant |
| REP | `GET /reports/profit?from=&to=&branchId=&channel=&skuId=` | Doanh thu, giá vốn, lợi nhuận gộp | Owner |
| REP | `GET /reports/inventory-value`, `/reports/top-selling`, `/reports/slow-moving` | Tồn kho và tốc độ bán | Owner, Staff |
| REP | `GET /alerts/stock`; `PATCH /alerts/stock/{id}/read` | Cảnh báo tồn thấp | Owner, Staff |

### Bảng mã lỗi

Mọi lỗi trả về cùng một dạng JSON theo mẫu của giáo viên; mã gồm tiền tố module + mã HTTP + số thứ tự. Lỗi validation kèm danh sách trường sai trong `details`.

```json
{
  "errorCode": "IV40901",
  "message": "Không đủ hàng khả dụng cho SKU AO-THUN-DEN-M (còn 2)",
  "details": []
}
```

| Mã | HTTP | Ý nghĩa |
| --- | --- | --- |
| `AU40001` | 400 | Dữ liệu đăng ký/đăng nhập không hợp lệ |
| `AU40101` | 401 | Sai thông tin đăng nhập, thiếu token, token sai hoặc hết hạn |
| `AU40301` | 403 | Không đủ quyền |
| `AU40302` | 403 | Tài khoản bị khóa |
| `AU40401` | 404 | Không tìm thấy người dùng/chi nhánh |
| `AU40901` | 409 | Email hoặc tên chi nhánh đã tồn tại |
| `PR40001` | 400 | Dữ liệu sản phẩm/SKU/giá không hợp lệ |
| `PR40401` | 404 | Không tìm thấy sản phẩm, SKU, danh mục, thương hiệu |
| `PR40901` | 409 | Trùng mã SKU hoặc mã vạch |
| `PR40902` | 409 | Không xóa được vì còn dữ liệu liên quan |
| `IV40001` | 400 | Số lượng, đơn giá hoặc chi nhánh không hợp lệ |
| `IV40401` | 404 | Không tìm thấy phiếu nhập, phiếu chuyển, phiên kiểm kê |
| `IV40901` | 409 | Không đủ hàng khả dụng |
| `IV40902` | 409 | Chứng từ đã xác nhận/duyệt, không sửa được |
| `OD40001` | 400 | Dữ liệu đơn hàng hoặc payload webhook không hợp lệ |
| `OD40101` | 401 | API key webhook sai |
| `OD40401` | 404 | Không tìm thấy đơn |
| `OD40901` | 409 | Chuyển trạng thái không hợp lệ |
| `OD40902` | 409 | Đơn trùng (cùng kênh và mã đơn ngoài) |
| `RP40001` | 400 | Tham số báo cáo không hợp lệ (khoảng ngày sai) |
| `SY50001` | 500 | Lỗi hệ thống, không trả chi tiết kỹ thuật ra ngoài |

## 5. Vai trò 7 thành viên

Mỗi người sở hữu trọn một module từ thiết kế đến test. M1 là nhóm trưởng. Thay M1–M7 bằng tên thật khi phân công.

| Thành viên | Vai trò | Phụ trách chính | Phụ trách thêm |
| --- | --- | --- | --- |
| M1 | Nhóm trưởng, Scrum Master, phân tích nghiệp vụ | URS, RTM, backlog, họp hàng ngày, demo, làm việc với giáo viên | Kế hoạch test, hướng dẫn sử dụng, báo cáo cuối |
| M2 | Trưởng kỹ thuật backend + DevOps | Khung dự án, AUTH/JWT/RBAC, tenant, xử lý lỗi chung, Swagger, Docker, CI/CD | Duyệt mã backend |
| M3 | Backend: sản phẩm và báo cáo | PROD, REP | Postman collection |
| M4 | Backend: tồn kho và giá vốn | INV, COST | Thiết kế dữ liệu, dữ liệu mẫu |
| M5 | Backend: đơn hàng và chống bán vượt | ORD, RSE, checkout POS, SIM | Test đồng thời (Flash Sale) |
| M6 | Frontend: Admin Dashboard | Khung React, đăng nhập, sản phẩm, tồn kho, đơn hàng, báo cáo | Wireframe Admin |
| M7 | Frontend: POS PWA, realtime, QA | POS, PWA, thông báo WebSocket | Test end-to-end giao diện |

Mọi thành viên viết unit test cho module của mình (mục tiêu ≥ 80% cho sổ cái, giữ hàng, giá vốn).

## 6. Tổng quan 5 sprint

Thứ tự đi từ nền tảng → dữ liệu gốc → nghiệp vụ lõi → bán hàng tại POS → kiểm thử và đóng gói.

| Sprint | Mục tiêu | Kết quả demo được |
| --- | --- | --- |
| 1 | Phân tích, thiết kế, dựng nền tảng và đăng nhập | URS/RTM, kiến trúc, ERD; dự án chạy được; đăng ký tenant, đăng nhập JWT, phân quyền, Swagger; khung React có trang đăng nhập |
| 2 | Sản phẩm, sổ cái tồn kho, nhập kho, giá vốn | CRUD sản phẩm/SKU/mã vạch/giá; phiếu nhập kho ghi sổ cái và tính giá vốn; Admin xem sản phẩm và tồn kho |
| 3 | Đơn hàng và chống bán vượt | Order Hub + state machine; giữ/trừ/nhả hàng có khóa; chuyển kho, kiểm kê; simulator; test 50 đơn đồng thời qua; Admin quản lý đơn; báo cáo lợi nhuận |
| 4 | POS, realtime, tác vụ nền, báo cáo | POS PWA bán hàng trọn vòng (tìm, quét mã, thanh toán, in); thông báo đơn mới realtime; tự hủy đơn giữ quá hạn; dashboard doanh thu, lợi nhuận; báo cáo tồn kho, cảnh báo tồn thấp |
| 5 | Kiểm thử, hoàn thiện, đóng gói, tài liệu | Test ≥ 80% module lõi; Flash Sale trên bản đóng gói; Docker Compose + seed data; CI/CD; bộ tài liệu và bản nộp cuối |

## 7. Công việc chi tiết từng sprint

Mỗi sprint có bảng đầu việc theo thành viên, các phụ thuộc cần lưu ý và kịch bản nghiệm thu phải chạy được khi demo cuối sprint. Mã FR/NFR theo mục 2 và 3.

### Sprint 1 – Phân tích, thiết kế, nền tảng

| M | Đầu việc | Sản phẩm bàn giao |
| --- | --- | --- |
| M1 | (1) Họp chốt phạm vi với giáo viên dựa trên mục 2–3. (2) Viết URS: tác nhân, use case theo module, yêu cầu chức năng và phi chức năng. (3) Lập RTM khung: FR/NFR → module → người phụ trách. (4) Tạo backlog user story trên GitHub Projects, ước lượng story point. (5) Thiết lập quy trình: họp ngắn hằng ngày, review, retro, mẫu báo cáo sprint | URS v1, RTM v1, backlog |
| M2 | (1) Tạo repo, nhánh `main`/`develop`, bảo vệ nhánh. (2) Khởi tạo Spring Boot 3 (Maven) theo 4 lớp `domain`/`application`/`infrastructure`/`web`. (3) PostgreSQL + Flyway, profile `dev`/`test`. (4) Bảng `tenant`, `branch`, `app_user`, `refresh_token`. (5) API register/login/refresh/logout/me; JWT filter; `@PreAuthorize` (FR-AUTH-01..03). (6) `TenantContext` + Hibernate `@Filter`. (7) `GlobalExceptionHandler` trả `{errorCode, message}`. (8) springdoc với nút Authorize Bearer | API AUTH chạy trên Swagger, tài liệu kiến trúc |
| M3 | (1) Thiết kế bảng `category`, `brand`, `product`, `sku`. (2) Entity, repository, migration. (3) API chi nhánh (FR-AUTH-04) và nhân viên (FR-AUTH-05). (4) Viết đặc tả OpenAPI cho PROD để frontend làm trước | API chi nhánh, nhân viên; đặc tả API PROD |
| M4 | (1) Thiết kế toàn bộ mô hình dữ liệu (24 bảng, mục 4). (2) Chi tiết `stock_balance`, `inventory_transaction`, phiếu nhập, chuyển kho, kiểm kê kèm ràng buộc `CHECK`. (3) Tài liệu công thức giá vốn + 10 ví dụ tính tay làm dữ liệu test. (4) Trigger chặn `UPDATE`/`DELETE` sổ cái | Mô hình dữ liệu v1, migration INV |
| M5 | (1) Đặc tả chuyển trạng thái đơn (mục 4). (2) Thiết kế luồng giữ hàng và thanh toán POS: từng bước, điểm khóa, điểm hoàn tác. (3) Thiết kế mô hình đơn chung + bảng ánh xạ payload Shopee/TikTok Shop. (4) Cùng M2 chốt bảng mã lỗi | Tài liệu thiết kế ORD/RSE, bảng mã lỗi |
| M6 | (1) Wireframe Admin: đăng nhập, sản phẩm, kho, đơn, báo cáo, cài đặt. (2) Khởi tạo React + Vite + TypeScript, router, layout, thư viện UI. (3) Lớp gọi API (axios) tự gắn token, tự làm mới token khi hết hạn. (4) Trang đăng nhập, đăng ký cửa hàng; chặn trang theo vai trò | Admin đăng nhập được với API thật |
| M7 | (1) Wireframe POS: màn bán hàng, thanh toán, hóa đơn. (2) Khởi tạo app POS + manifest PWA. (3) Thử thư viện quét mã vạch bằng camera và máy quét USB (nhập như bàn phím). (4) Bộ component dùng chung (nút, bảng, form, thông báo) cho cả Admin và POS | Khung POS chạy, bộ component |

**Phụ thuộc:** M3, M4, M5 cần khung dự án của M2 (việc (2)–(3) làm trước tiên). M6, M7 cần đặc tả API đăng nhập.

**Nghiệm thu Sprint 1:** đăng ký cửa hàng mới trên Admin → đăng nhập → dùng token gọi `GET /me` trên Swagger thành công; gọi API Owner bằng tài khoản Cashier nhận 403 `AU40301`; URS và mô hình dữ liệu được cả nhóm duyệt.

### Sprint 2 – Sản phẩm, sổ cái, nhập kho, giá vốn

| M | Đầu việc | Sản phẩm bàn giao |
| --- | --- | --- |
| M1 | (1) Kế hoạch kiểm thử: phạm vi, công cụ, tiêu chí đạt. (2) Test case cho AUTH, PROD, INV (tối thiểu 30 case, mỗi FR có case đúng và case lỗi). (3) Test thủ công API Sprint 1, ghi lỗi lên GitHub Issues. (4) Cập nhật RTM | Kế hoạch test v1, test case v1, RTM v2 |
| M2 | (1) Dockerfile backend + `docker-compose.yml` (app + PostgreSQL). (2) GitHub Actions: build, chạy test, báo cáo JaCoCo cho mọi pull request. (3) Test cô lập tenant: 2 tenant, đọc chéo phải ra 404 (NFR-TENANT-01). (4) Test ArchUnit kiểm tra phụ thuộc giữa các lớp. (5) Duyệt pull request backend | Pipeline CI chạy, test tenant + ArchUnit |
| M3 | (1) API danh mục, thương hiệu (FR-PROD-01). (2) API sản phẩm + tạo nhiều SKU biến thể một lần (FR-PROD-02). (3) Sinh EAN-13 có check digit, tra theo mã vạch (FR-PROD-03). (4) API giá (FR-PROD-04). (5) Tìm kiếm, lọc, phân trang (FR-PROD-05). (6) Validation bằng Bean Validation, trả `PR40001`. (7) Unit test service, thư mục Postman PROD | API PROD đầy đủ, Postman PROD |
| M4 | (1) `InventoryLedgerService.post(...)`: khóa dòng tồn, cập nhật `on_hand`, ghi sổ cái trong cùng giao dịch (FR-INV-01). (2) API nhà cung cấp. (3) API phiếu nhập: tạo, sửa khi Nháp, xác nhận (FR-INV-02). (4) Tính lại `avg_cost` khi xác nhận (FR-COST-01). (5) API xem tồn và sổ cái. (6) Unit test giá vốn với 10 ví dụ đã tính tay | API nhập kho, sổ cái, giá vốn |
| M5 | (1) Entity `orders`, `order_item`, `order_status_history`, `payment` + migration. (2) Lớp `OrderStateMachine` thuần domain, unit test đủ 7 bước hợp lệ và các bước bị chặn. (3) API tạo đơn tay (Draft). (4) Script seed data: 2 tenant, 3 chi nhánh, 5 danh mục, 50 SKU, 3 nhà cung cấp, tài khoản mỗi vai trò | State machine có test, seed data |
| M6 | (1) Màn danh mục, thương hiệu. (2) Màn sản phẩm: danh sách có tìm/lọc, form tạo sản phẩm sinh nhiều biến thể, in tem mã vạch. (3) Màn phiếu nhập: tạo, sửa, xác nhận. (4) Màn tồn kho và sổ cái có lọc chi nhánh, SKU, khoảng ngày | Admin PROD + nhập kho |
| M7 | (1) Màn bán hàng: ô tìm, kết quả, giỏ hàng (sửa số lượng, xóa dòng, tổng tiền). (2) Quét mã + Enter thêm vào giỏ; quét lại tăng số lượng. (3) Phím tắt F2/F9/Esc. (4) Dùng dữ liệu giả theo đặc tả OpenAPI khi API chưa xong | POS tìm kiếm và giỏ hàng |

**Phụ thuộc:** M4 cần bảng `sku` của M3. M6 cần API của M3, M4 (làm giao diện với dữ liệu giả trước).

**Nghiệm thu Sprint 2:** tạo sản phẩm 3 biến thể trên Admin → nhập 2 phiếu với giá khác nhau → `avg_cost` đúng công thức, sổ cái có 2 dòng IN với `balance_after` đúng; gọi `UPDATE` trực tiếp vào sổ cái bị từ chối; CI chạy xanh.

### Sprint 3 – Đơn hàng và chống bán vượt

| M | Đầu việc | Sản phẩm bàn giao |
| --- | --- | --- |
| M1 | (1) Test case cho ORD, RSE, COST, chuyển kho, kiểm kê. (2) Viết kịch bản Flash Sale (dữ liệu, số luồng, kết quả mong đợi). (3) Nháp tài liệu thiết kế chi tiết: mô tả lớp, luồng giữ hàng, luồng thanh toán. (4) Retro giữa dự án, điều chỉnh backlog | Test case v2, tài liệu thiết kế nháp |
| M2 | (1) Rà soát `@Transactional`, mức cô lập và hoàn tác cho ORD/RSE/POS (NFR-SEC-02). (2) Thêm index `(tenant_id, created_at)`, `(tenant_id, sku_id)` (NFR-TENANT-02). (3) Test giả lập lỗi giữa chuỗi Tạo đơn → Giữ → Trừ. (4) Duyệt mã module ORD/RSE | Test ACID, index |
| M3 | (1) API báo cáo lợi nhuận từ `order_item.cost_price` với bộ lọc ngày, chi nhánh, kênh, SKU (FR-REP-01). (2) Xuất CSV. (3) Test số liệu báo cáo với bộ dữ liệu tính tay. (4) Thư mục Postman REP | API báo cáo lợi nhuận |
| M4 | (1) Chuyển kho: tạo, xuất (ghi OUT), nhận (ghi IN) (FR-INV-03). (2) Kiểm kê: tạo phiên, nhập số đếm, duyệt → bút toán Điều chỉnh (FR-INV-04). (3) Cùng M5: hàm chụp `cost_price` khi đơn Confirmed (FR-COST-02). (4) Unit test chuyển kho và kiểm kê | API chuyển kho, kiểm kê |
| M5 | (1) `ReservationService`: khóa `FOR UPDATE` theo thứ tự `sku_id`, kiểm tra `available`, tăng `reserved` (FR-RSE-01, 02). (2) API reserve/confirm/complete/cancel gọi state machine + trừ/nhả hàng (FR-ORD-02, 03; FR-RSE-03). (3) Webhook `/webhooks/{channel}`: xác thực API key, ánh xạ payload, chống trùng (FR-ORD-01). (4) API simulator gửi N đơn song song (FR-SIM-01). (5) **Test 50 luồng đồng thời vào 1 SKU còn 1 cái** (NFR-PERF-02) | API đơn hàng, webhook, simulator, test Flash Sale |
| M6 | (1) Màn đơn hàng: danh sách có lọc, chi tiết, lịch sử trạng thái. (2) Nút Giữ/Duyệt/Hoàn tất/Hủy (hủy bắt nhập lý do), chỉ hiện nút hợp lệ với trạng thái. (3) In phiếu giao. (4) Màn chuyển kho, kiểm kê. (5) Màn báo cáo lợi nhuận (bảng + biểu đồ) | Admin đơn hàng, kho, báo cáo |
| M7 | (1) POS nối API thật: tìm sản phẩm, tra mã vạch kèm `available`. (2) Chặn số lượng vượt `available`, báo số còn lại (FR-POS-02). (3) Service worker cache tài nguyên tĩnh. (4) Màn Webhook Simulator trên Admin: chọn sàn, SKU, số đơn, xem kết quả thành công/thất bại | POS nối API, màn Simulator |

**Phụ thuộc:** chụp giá vốn của M4 gắn vào bước Confirmed của M5. Báo cáo của M3 cần đơn Confirmed có `cost_price`, nên M5 làm việc (1)–(2) trước.

**Nghiệm thu Sprint 3:** trên màn Simulator gửi 50 đơn cùng lúc vào SKU còn 1 cái → đúng 1 đơn Reserved, 49 đơn lỗi `IV40901`, `available` = 0; một đơn đi đủ Reserved → Confirmed → Completed trên Admin, sổ cái có dòng SALE, báo cáo lợi nhuận khớp tính tay.

### Sprint 4 – POS, thời gian thực, tác vụ nền, báo cáo

| M | Đầu việc | Sản phẩm bàn giao |
| --- | --- | --- |
| M1 | (1) UAT theo kịch bản đầu-cuối: nhập hàng → bán tại POS → đơn sàn → báo cáo. (2) Hướng dẫn sử dụng v1 cho Admin và POS. (3) Theo dõi và ưu tiên lỗi. (4) Cập nhật RTM | Báo cáo UAT, hướng dẫn v1, RTM v3 |
| M2 | (1) WebSocket STOMP xác thực JWT lúc kết nối, kênh theo tenant (FR-SIM-02). (2) Phát sự kiện đơn mới và cảnh báo tồn sau khi commit. (3) Refresh token xoay vòng, lưu dạng băm (NFR-SEC-01). (4) Dockerfile frontend (nginx), compose đủ 4 dịch vụ: backend, Admin, POS, PostgreSQL | WebSocket, compose đầy đủ |
| M3 | (1) API giá trị tồn, top bán chạy, hàng tồn chậm (FR-REP-02). (2) Swagger: mô tả, ví dụ request/response và ràng buộc validation cho mọi DTO. (3) Bổ sung Postman cho INV, ORD | API báo cáo tồn, Swagger đầy đủ |
| M4 | (1) Sinh cảnh báo khi `available` ≤ `reorder_point`, tự đóng khi nhập vượt ngưỡng (FR-REP-03). (2) API cảnh báo. (3) Phân trang keyset cho sổ cái. (4) Script sinh 1 triệu dòng sổ cái để đo hiệu năng | Cảnh báo tồn, dữ liệu hiệu năng |
| M5 | (1) `POST /pos/checkout`: tạo đơn → giữ → xác nhận → hoàn tất → ghi thanh toán trong một giao dịch (FR-POS-04). (2) `@Scheduled` mỗi phút hủy đơn Reserved quá hạn, nhả hàng. (3) Job mỗi đêm ghi `daily_report` (FR-SIM-03). (4) Test tích hợp cho checkout và job | API checkout, tác vụ nền |
| M6 | (1) Dashboard: doanh thu, lợi nhuận, số đơn theo trạng thái, cảnh báo tồn. (2) Nhận WebSocket: popup + âm thanh khi có đơn mới. (3) Ẩn menu theo vai trò. (4) Màn nhân viên, chi nhánh, cài đặt thời hạn giữ hàng. (5) Màn báo cáo tồn kho | Admin dashboard, thông báo realtime |
| M7 | (1) Màn thanh toán: tiền mặt (tính tiền thối), QR (hiển thị mã, xác nhận đã nhận). (2) Gọi checkout, xử lý lỗi hết hàng. (3) Mẫu hóa đơn 80 mm, in bằng `window.print()` (FR-POS-03). (4) Cài đặt PWA. (5) Nhận thông báo đơn mới trên POS | POS bán hàng trọn vòng |

**Phụ thuộc:** POS của M7 cần API checkout của M5. Thông báo trên Admin/POS cần WebSocket của M2.

**Nghiệm thu Sprint 4:** tại POS quét 2 mã vạch → F9 → nhập tiền khách → in hóa đơn, tối đa 3 thao tác sau khi quét; tồn kho và báo cáo cập nhật ngay; gửi webhook từ Simulator thì Admin hiện popup không cần tải lại; đơn giữ quá hạn tự chuyển Cancelled và nhả hàng.

### Sprint 5 – Kiểm thử, hoàn thiện, đóng gói

| M | Đầu việc | Sản phẩm bàn giao |
| --- | --- | --- |
| M1 | (1) Hoàn thiện bộ tài liệu: URS, RTM cuối, kiến trúc, thiết kế chi tiết (kèm sơ đồ), kế hoạch triển khai, tài liệu kiểm thử có kết quả, cài đặt và hướng dẫn. (2) Slide và kịch bản demo. (3) Tổng duyệt bảo vệ, phân công ai trình bày phần nào | Bộ tài liệu nộp, slide |
| M2 | (1) Pipeline CI: build image, chạy toàn bộ test, chặn merge khi coverage dưới ngưỡng (NFR-MAINT-02). (2) Gói demo: `docker compose up` dựng đủ hệ thống kèm seed data. (3) Cài thử trên máy sạch. (4) README hướng dẫn chạy. (5) Healthcheck + tự khởi động lại (NFR-USA-01) | Gói demo chạy bằng một lệnh |
| M3 | (1) Rà soát validation và mã lỗi toàn bộ API theo bảng mã lỗi. (2) Postman collection cuối: mỗi endpoint có ít nhất 1 case đúng và 1 case lỗi, script kiểm tra status code và `errorCode`, tự lấy token sau đăng nhập. (3) Xuất environment Postman | Postman collection + environment |
| M4 | (1) Nâng coverage sổ cái và giá vốn ≥ 80%. (2) Đo báo cáo < 2 s và truy vấn sổ cái trên 1 triệu dòng (NFR-PERF-01, NFR-TENANT-02). (3) Sửa lỗi phần kho | Báo cáo coverage, kết quả đo |
| M5 | (1) Nâng coverage giữ hàng, trừ/nhả hàng ≥ 80%. (2) Chạy lại Flash Sale trên bản Docker. (3) Đo thời gian tạo đơn < 500 ms. (4) Sửa lỗi phần đơn hàng | Báo cáo coverage, kết quả Flash Sale |
| M6 | (1) Hoàn thiện Admin: hiển thị lỗi theo `errorCode`, trạng thái đang tải, trạng thái rỗng, hộp xác nhận trước khi xóa/hủy. (2) Kiểm thử trên Chrome và Edge. (3) Phần hướng dẫn Admin có ảnh chụp | Admin bản cuối |
| M7 | (1) Test E2E bằng Playwright cho các luồng: đăng nhập, tạo sản phẩm, nhập kho, bán POS, xử lý đơn sàn. (2) Đo p95 tìm/quét < 200 ms. (3) Responsive máy tính, tablet, điện thoại (NFR-USA-02). (4) Phần hướng dẫn POS | Báo cáo E2E, POS bản cuối |

**Phụ thuộc:** tài liệu của M1 cần kết quả test và đo hiệu năng của M4, M5, M7; gói demo của M2 cần mọi nhánh đã merge.

**Nghiệm thu Sprint 5:** máy mới chỉ cần `docker compose up` là chạy được cả Backend, Admin, POS và dữ liệu mẫu; CI xanh với coverage module lõi ≥ 80%; Postman collection chạy hết không lỗi; đủ bộ tài liệu nộp.

## 8. Tiêu chí hoàn thành

Mỗi hạng mục chỉ được xem là xong khi đạt đủ:

- Mã biên dịch không lỗi, CI xanh, đã được một thành viên khác duyệt.
- Có unit test; API có mô tả Swagger và test Postman.
- Trả lỗi đúng định dạng `{errorCode, message}` và đúng mã HTTP (200, 201, 400, 401, 403, 404, 500).
- RTM được cập nhật: yêu cầu → thiết kế → mã → test.
