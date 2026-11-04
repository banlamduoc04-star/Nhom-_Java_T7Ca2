<div align="center">

# 📦 OISM

**Omnichannel Inventory and Sales Management System**
Hệ thống quản lý bán hàng và tồn kho đa kênh

</div>

## 📖 Giới thiệu

OISM là hệ thống quản lý bán hàng và tồn kho đa kênh theo mô hình SaaS nhiều cửa hàng (multi-tenant). Hệ thống gom đơn từ POS, Admin và các sàn (Shopee, TikTok Shop) về một nơi, theo dõi tồn kho bằng sổ cái chỉ ghi thêm, tính giá vốn bình quân gia quyền và chống bán vượt tồn kho khi nhiều đơn vào cùng lúc.

Dự án chia thành **5 sprint** với **7 thành viên (M1–M7)**. Kế hoạch đầy đủ: [`docx/Kế hoạch dự án OISM – 5 Sprint, 7 thành viên.md`](docx/).

## 👥 Vai trò người dùng

| Vai trò | Quyền |
|---|---|
| **Owner** | Toàn quyền trong cửa hàng |
| **Staff** | Kho, đơn hàng, xem báo cáo tồn |
| **Cashier** | Chỉ POS và tra cứu sản phẩm |

## ✨ Chức năng chính

| Module | Nội dung |
|---|---|
| **AUTH** | Đăng ký cửa hàng (tenant), đăng nhập JWT, phân quyền theo vai trò, chi nhánh/kho, tài khoản nhân viên |
| **PROD** | Danh mục nhiều cấp, thương hiệu, sản phẩm và biến thể (SKU), mã vạch EAN-13, giá lẻ/sỉ, tìm kiếm và lọc |
| **INV** | Sổ cái tồn kho chỉ ghi thêm, phiếu nhập, chuyển kho 2 bước, kiểm kê, nhà cung cấp |
| **COST** | Giá vốn bình quân gia quyền, chụp giá vốn vào đơn |
| **ORD** | Trung tâm đơn hàng đa kênh, máy trạng thái đơn, lịch sử xử lý |
| **RSE** | Giữ hàng, trừ/nhả hàng bằng khóa pessimistic, không bán vượt tồn |
| **POS** | Bán nhanh bằng mã vạch, phím tắt, thanh toán tiền mặt/QR, in hóa đơn 80 mm, PWA |
| **REP** | Doanh thu, lợi nhuận gộp, tồn kho, hàng bán chạy/tồn chậm, cảnh báo tồn thấp |
| **SIM** | Webhook Simulator Shopee/TikTok, thông báo realtime (WebSocket), tác vụ nền |

## 🛠️ Công nghệ sử dụng

| Lớp | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA/Hibernate |
| Cơ sở dữ liệu | PostgreSQL 16, Flyway migration |
| Xác thực | JWT (access 60 phút + refresh 7 ngày xoay vòng), mật khẩu BCrypt |
| Cô lập tenant | Cột `tenant_id` + Hibernate `@Filter` theo từng request |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Frontend | Admin Dashboard và POS (Next.js/React, TypeScript) |
| Kiểm thử | JUnit 5, Mockito (Testcontainers, Playwright ở các sprint sau) |
| Triển khai | Docker, docker-compose, GitHub Actions |

## 🚀 Chạy bằng Docker (khuyến nghị)

Chỉ cần cài **Docker Desktop**, không cần cài Java hay Maven.

```bash
cd omnichannel-oism
docker compose up -d --build
```

Lần đầu build mất vài phút. Sau đó:

| Dịch vụ | Địa chỉ |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API | http://localhost:8080/api/v1 |
| PostgreSQL | `localhost:5432`, DB `oism_db`, user `postgres` |

Các bảng do Flyway tự tạo khi backend khởi động.

**Thử nhanh:** trong Swagger gọi `POST /auth/register` → `POST /auth/login` → bấm **Authorize** và dán `accessToken` → `GET /me`.

Lệnh hay dùng (chạy trong thư mục `omnichannel-oism`):

```bash
docker compose logs -f backend          # xem log backend
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose down                     # dừng, giữ dữ liệu
docker compose down -v                  # dừng và xóa sạch dữ liệu
```

**Chạy cả giao diện** (tùy chọn): `docker compose --profile frontend up -d --build` rồi mở Admin ở http://localhost:3000 và POS ở http://localhost:3001. Lưu ý: `frontend-admin` hiện chưa build được do trùng route `/products` giữa `app/products` và `app/(dashboard)/products`, cần M6 thống nhất lại.

**Biến môi trường** (đặt trong file `.env` cạnh `docker-compose.yml`; mặc định đã chạy được cho dev):

| Biến | Ý nghĩa |
|---|---|
| `POSTGRES_PASSWORD` | Mật khẩu PostgreSQL (mặc định `postgres`) |
| `OISM_JWT_SECRET` | Khóa ký JWT, tối thiểu 32 byte. **Bắt buộc đặt riêng khi triển khai thật** |

## 💻 Phát triển backend bằng IDE

```bash
cd omnichannel-oism
docker compose up -d postgres          # chỉ chạy CSDL
cd backend
./mvnw clean verify                    # build và chạy test (Windows: .\mvnw.cmd clean verify)
./mvnw spring-boot:run                 # chạy backend, profile dev
```

> Nên đặt repo ở đường dẫn **không dấu, không khoảng trắng** để `spring-boot:run` không lỗi `ClassNotFoundException`. Bản Docker không bị ảnh hưởng.

## 🏗️ Kiến trúc backend

Clean Architecture 4 lớp trong `omnichannel-oism/backend/src/main/java/com/oism`:

| Lớp | Nội dung |
|---|---|
| `domain` | Entity, repository, `ErrorCode`, `BusinessException` |
| `application` | Use case/service (`AuthService`), command, result |
| `infrastructure` | JWT, Security, tenant filter, OpenAPI |
| `web` | Controller, DTO, `GlobalExceptionHandler` |

Lỗi trả về thống nhất dạng `{"errorCode": "IV40901", "message": "...", "details": []}` với mã `<TIỀN TỐ><HTTP><SỐ>` (AU, PR, IV, OD, RP, SY).

## 📁 Cấu trúc thư mục

```text
.
├── README.md
├── docx/                      # Kế hoạch dự án OISM (docx + md)
├── diagram/                   # Sơ đồ thiết kế (draw.io, docx)
└── omnichannel-oism/
    ├── docker-compose.yml     # PostgreSQL + backend (+ frontend, profile "frontend")
    ├── backend/               # Spring Boot 3 (Dockerfile, pom.xml, src/)
    │   └── legacy-reference/  # Code cũ tham khảo, không nằm trong build
    ├── frontend-admin/        # Admin Dashboard
    └── frontend-pos/          # POS
```

## 🗓️ Lộ trình

| Sprint | Mục tiêu |
|---|---|
| 1 | Phân tích, thiết kế, nền tảng backend, đăng ký/đăng nhập JWT, Swagger ✅ |
| 2 | Sản phẩm, sổ cái tồn kho, nhập kho, giá vốn |
| 3 | Đơn hàng, chống bán vượt, chuyển kho, kiểm kê, simulator |
| 4 | POS, realtime, tác vụ nền, báo cáo |
| 5 | Kiểm thử, hoàn thiện, Docker Compose + seed data, CI/CD, tài liệu |

## 🤝 Quy tắc làm việc

- Nhánh: `main`, `develop`, `feature/*`.
- Mỗi pull request cần 1 người duyệt và CI xanh (biên dịch + test) mới được merge.
- Thư mục `docs/` là tài liệu nội bộ, không đẩy lên GitHub.
