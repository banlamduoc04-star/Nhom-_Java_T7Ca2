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

### 3.1. Tài khoản dữ liệu mẫu (19 tài khoản)

Đây là các tài khoản nạp sẵn trong CSDL Docker bằng script `scripts/du-lieu-mau/nap-du-lieu.ps1` (chỉ dùng cho dev/demo trên máy local). Đăng nhập bằng email hoặc số điện thoại. Mật khẩu chung: **`admin123`**.

| Cửa hàng | Vai trò | Chi nhánh | Họ tên | Email | Mật khẩu | Đăng nhập ở |
|---|---|---|---|---|---|---|
| Giày Dép Hưng Thịnh | Owner |  | Trần Văn Hưng | `hung.tran@giayhungthinh.vn` | `admin123` | Admin (bán được cả trên POS) |
| Giày Dép Hưng Thịnh | Staff |  | Nguyễn Thị Thu Hà | `thuha.kho@giayhungthinh.vn` | `admin123` | Admin |
| Giày Dép Hưng Thịnh | Staff |  | Lê Minh Đức | `duc.lm@giayhungthinh.vn` | `admin123` | Admin |
| Giày Dép Hưng Thịnh | Cashier | Hưng Thịnh Cầu Giấy | Phạm Ngọc Ánh | `ngocanh.cg@giayhungthinh.vn` | `admin123` | POS |
| Giày Dép Hưng Thịnh | Cashier | Hưng Thịnh Cầu Giấy | Đỗ Quang Huy | `huy.do@giayhungthinh.vn` | `admin123` | POS |
| Giày Dép Hưng Thịnh | Cashier | Hưng Thịnh Phố Huế | Vũ Thị Mai | `mai.vu@giayhungthinh.vn` | `admin123` | POS |
| Giày Dép Hưng Thịnh | Cashier | Hưng Thịnh Phố Huế | Hoàng Anh Tuấn | `tuan.ha@giayhungthinh.vn` | `admin123` | Đã nghỉ việc, tài khoản bị khóa |
| Giày Dép Hưng Thịnh | Cashier | Hưng Thịnh Phố Huế | Bùi Khánh Linh | `khanhlinh.ph@giayhungthinh.vn` | `admin123` | POS |
| Thời trang Mộc Lan | Owner |  | Lâm Ngọc Lan | `lan.lam@moclanfashion.vn` | `admin123` | Admin (bán được cả trên POS) |
| Thời trang Mộc Lan | Staff |  | Trịnh Bảo Ngọc | `baongoc@moclanfashion.vn` | `admin123` | Admin |
| Thời trang Mộc Lan | Staff |  | Nguyễn Hoàng Phúc | `phuc.nh@moclanfashion.vn` | `admin123` | Admin |
| Thời trang Mộc Lan | Cashier | Mộc Lan Nguyễn Trãi | Đặng Thùy Linh | `thuylinh.nt@moclanfashion.vn` | `admin123` | POS |
| Thời trang Mộc Lan | Cashier | Mộc Lan Nguyễn Trãi | Huỳnh Gia Bảo | `giabao.h@moclanfashion.vn` | `admin123` | POS |
| Thời trang Mộc Lan | Cashier | Mộc Lan Thủ Đức | Lý Thanh Trúc | `truc.ly@moclanfashion.vn` | `admin123` | POS |
| Thời trang Mộc Lan | Cashier | Mộc Lan Thủ Đức | Tô Minh Khang | `khang.to@moclanfashion.vn` | `admin123` | POS |
| Tạp hóa Phúc An | Owner |  | Hồ Văn Phúc | `phuc.ho@taphoaphucan.vn` | `admin123` | Admin (bán được cả trên POS) |
| Tạp hóa Phúc An | Staff |  | Lê Thị Kim Oanh | `kimoanh@taphoaphucan.vn` | `admin123` | Admin |
| Tạp hóa Phúc An | Cashier | Phúc An Hải Châu | Hồ Thị An | `an.ho@taphoaphucan.vn` | `admin123` | POS |
| Tạp hóa Phúc An | Cashier | Phúc An Hải Châu | Ngô Văn Tài | `tai.ngo@taphoaphucan.vn` | `admin123` | POS |

> Chạy lại `nap-du-lieu.ps1` thì dữ liệu bán hàng được sinh mới nhưng email và mật khẩu `admin123` giữ nguyên. Nếu chạy `docker compose down -v` (xóa CSDL) thì cần chạy lại `nap-du-lieu.ps1` để có lại các tài khoản này.

### 3.2. Tạo tài khoản mới

1. Mở Admin http://localhost:3000 → **Đăng ký** để tạo cửa hàng. Tài khoản vừa đăng ký là **Owner**.
2. Đăng nhập Owner, vào **Nhân viên** để tạo **Staff** (dùng Admin) và **Cashier** (dùng POS, gán chi nhánh).

## 4. Lệnh hay dùng

```bash
docker compose logs -f backend          # xem log backend
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose down                     # dừng, giữ dữ liệu
docker compose down -v                  # dừng và XÓA sạch dữ liệu
```

## 5. Thử nhanh API bằng Swagger

1. Mở http://localhost:8080/swagger-ui.html.
2. `POST /auth/login` với body `{"identifier": "<email>", "password": "<mật khẩu>"}` (lấy ở mục 3.1) → copy `accessToken`.
3. Bấm **Authorize**, dán token (không gõ chữ `Bearer`).
4. Thử `GET /me`.




## 6. Chạy kiểm thử postman 

# 1. Dừng stack chính (KHÔNG thêm -v để giữ nguyên volume oism_pgdata hiện có)
docker compose down

# 2. Dựng 1 DB + Backend giả lập riêng biệt với tên project khác (-p oism-test)
#    Docker sẽ tạo volume riêng "oism-test_oism_pgdata", hoàn toàn trống và sạch
docker compose -p oism-test up -d --build

# 3. Đợi đến khi cột STATUS hiện "healthy"
docker compose -p oism-test ps