# Hướng dẫn chạy dự án OISM

Chỉ cần cài **Docker Desktop**, không cần Java, Maven hay Node.

## 1. Lệnh chạy

Chạy ở thư mục gốc repo.

**Chạy đủ hệ thống (Backend, Admin, POS):**

```bash
docker compose --profile frontend up -d --build
```

**Chỉ chạy backend + CSDL:**

```bash
docker compose up -d --build
```

Lần đầu build mất vài phút. Kiểm tra bằng `docker compose ps`: các container phải ở trạng thái `healthy`.

## 2. Đường link

| Dịch vụ | Địa chỉ |
|---|---|
| Admin | http://localhost:3000 |
| POS | http://localhost:3001 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API | http://localhost:8080/api/v1 |
| PostgreSQL | `localhost:5432`, DB `oism_db`, user `postgres`, mật khẩu `postgres` |

## 3. Tài khoản

### 3.1. Tài khoản đang dùng

Mật khẩu chung: **`admin123`** (chỉ dùng cho dev/demo trên máy local).

| Cửa hàng | Vai trò | Email | Đăng nhập ở |
|---|---|---|---|
| Shop Giày | Owner | `owner@shopgiay.vn` | Admin (bán được cả trên POS) |
| Shop Giày | Staff | `staff@shopgiay.vn` | Admin |
| Shop Giày | Cashier | `cashier@shopgiay.vn` | POS |
| Shop Quần Áo | Owner | `owner@shopquanao.vn` | Admin (bán được cả trên POS) |
| Shop Quần Áo | Staff | `staff@shopquanao.vn` | Admin |
| Shop Quần Áo | Cashier | `cashier@shopquanao.vn` | POS |

Mỗi shop đã có danh mục, thương hiệu, nhà cung cấp, 5 sản phẩm (13–15 SKU, có mã vạch EAN-13) và hàng nhập sẵn tại "Chi nhánh chính".

> Các tài khoản này nằm trong CSDL Docker của máy đã tạo. Chạy `docker compose down -v` hoặc dùng máy khác thì không còn: dùng dữ liệu mẫu ở mục 3.2 hoặc tạo lại theo mục 3.3.

### 3.2. Dữ liệu mẫu tự nạp (máy nào cũng có)

Đặt `OISM_SEED_ENABLED=true` khi chạy để backend tự nạp 2 cửa hàng, 3 chi nhánh, 50 SKU, 3 nhà cung cấp và phiếu nhập đã xác nhận. Nạp một lần (bỏ qua nếu đã có `owner.a@oism.vn`), không đụng tới các cửa hàng khác ở mục 3.1.

```powershell
$env:OISM_SEED_ENABLED="true"; docker compose --profile frontend up -d --build
```

Mật khẩu chung: **`Oism@12345`** (chỉ dùng cho dev/demo).

| Cửa hàng | Vai trò | Email |
|---|---|---|
| Demo Shop A | Owner | `owner.a@oism.vn` |
| Demo Shop A | Staff | `staff.a@oism.vn` |
| Demo Shop A | Cashier | `cashier.a@oism.vn` |
| Demo Shop B | Owner | `owner.b@oism.vn` |
| Demo Shop B | Cashier | `cashier.b@oism.vn` |

### 3.3. Tạo tài khoản mới

1. Mở Admin http://localhost:3000 → **Đăng ký** để tạo cửa hàng. Tài khoản vừa đăng ký là **Owner**.
2. Đăng nhập bằng tài khoản Owner, vào **Nhân viên** để tạo thêm tài khoản **Staff** (dùng Admin) và **Cashier** (dùng POS, gán chi nhánh).

## 4. Lệnh hay dùng

```bash
docker compose logs -f backend          # xem log backend
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose down                     # dừng, giữ dữ liệu
docker compose down -v                  # dừng và XÓA sạch dữ liệu
```

## 5. Thử nhanh API bằng Swagger

1. Mở http://localhost:8080/swagger-ui.html.
2. `POST /auth/login` với body `{"identifier": "owner@shopgiay.vn", "password": "admin123"}` → copy `accessToken`.
3. Bấm **Authorize**, dán token (không gõ chữ `Bearer`).
4. Thử `GET /me`.
