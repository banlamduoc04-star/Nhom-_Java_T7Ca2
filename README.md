
# 📦 OISM

**Omnichannel Inventory and Sales Management System**
Hệ thống quản lý bán hàng và tồn kho đa kênh

</div>

## 📖 Giới thiệu

OISM là hệ thống quản lý bán hàng và tồn kho đa kênh theo mô hình SaaS nhiều cửa hàng (multi-tenant). Hệ thống gom đơn từ POS, Admin và các sàn (Shopee, TikTok Shop) về một nơi, theo dõi tồn kho bằng sổ cái chỉ ghi thêm, tính giá vốn bình quân gia quyền và chống bán vượt tồn kho khi nhiều đơn vào cùng lúc.

Dự án chia thành **5 sprint** với **7 thành viên (M1–M7)**. Kế hoạch đầy đủ: [`docs/ke-hoach/ke-hoach-du-an-oism.md`](docs/ke-hoach/ke-hoach-du-an-oism.md).

## 👥 Vai trò người dùng

| Vai trò | Quyền |
| --- | --- |
| **Owner** | Toàn quyền trong cửa hàng |
| **Staff** | Kho, đơn hàng, xem báo cáo tồn |
| **Cashier** | Chỉ POS và tra cứu sản phẩm |

## ✨ Chức năng chính

| Module | Nội dung |
| --- | --- |
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
| --- | --- |
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA/Hibernate |
| Cơ sở dữ liệu | PostgreSQL 16, Flyway migration |
| Xác thực | JWT (access 60 phút + refresh 7 ngày xoay vòng), mật khẩu BCrypt |
| Cô lập tenant | Cột `tenant_id` + Hibernate `@Filter` theo từng request |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Frontend | Admin Dashboard và POS (React + Vite + TypeScript; Admin dùng Ant Design, POS là PWA) |
| Kiểm thử | JUnit 5, Mockito, Testcontainers, ArchUnit, Vitest (Playwright, JMeter ở Sprint 5) |
| Triển khai | Docker, docker-compose, GitHub Actions |

## 🚀 Chạy bằng Docker (khuyến nghị)

Chỉ cần cài **Docker Desktop**, không cần cài Java hay Maven.

```bash
docker compose up -d --build
```

Lần đầu build mất vài phút. Sau đó:

| Dịch vụ | Địa chỉ |
| --- | --- |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| API | <http://localhost:8080/api/v1> |
| PostgreSQL | `localhost:5432`, DB `oism_db`, user `postgres` |

Các bảng do Flyway tự tạo khi backend khởi động.

**Thử nhanh:** trong Swagger gọi `POST /auth/register` → `POST /auth/login` → bấm **Authorize** và dán `accessToken` → `GET /me`.

Lệnh hay dùng (chạy ở thư mục gốc repo):

```bash
docker compose logs -f backend          # xem log backend
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose down                     # dừng, giữ dữ liệu
docker compose down -v                  # dừng và xóa sạch dữ liệu
```

**Chạy cả giao diện** (tùy chọn): `docker compose --profile frontend up -d --build` rồi mở Admin ở <http://localhost:3000> và POS ở <http://localhost:3001>.

**Nạp dữ liệu mẫu** (2 cửa hàng, 50 SKU, tài khoản đủ 3 vai trò): đặt `OISM_SEED_ENABLED=true` khi chạy compose. Tài khoản và mật khẩu mẫu xem [Hướng dẫn chạy dự án OISM](docs/huong-dan/huong-dan-chay-du-an.md).

**Biến môi trường** (đặt trong file `.env` cạnh `docker-compose.yml`; mặc định đã chạy được cho dev):

| Biến | Ý nghĩa |
| --- | --- |
| `POSTGRES_PASSWORD` | Mật khẩu PostgreSQL (mặc định `postgres`) |
| `OISM_JWT_SECRET` | Khóa ký JWT, tối thiểu 32 byte. **Bắt buộc đặt riêng khi triển khai thật** |
| `OISM_SEED_ENABLED` | `true` để nạp dữ liệu mẫu demo (mặc định `false`) |

## 💻 Phát triển backend bằng IDE

```bash
docker compose up -d postgres          # chỉ chạy CSDL
cd backend
./mvnw clean verify                    # build + test (test tích hợp cần Docker; Windows: .\mvnw.cmd clean verify)
./mvnw spring-boot:run                 # chạy backend, profile dev
cd ../frontend-admin && npm install && npm run dev   # Admin, http://localhost:5173
cd ../frontend-pos && npm install && npm run dev     # POS
```

Chi tiết từng bước và xử lý lỗi: [Hướng dẫn chạy dự án OISM](docs/huong-dan/huong-dan-chay-du-an.md). Quy ước code cho cả nhóm: [Chuẩn cấu hình chung](docs/huong-dan/chuan-cau-hinh-chung.md).

> Nên đặt repo ở đường dẫn **không dấu, không khoảng trắng** để `spring-boot:run` không lỗi `ClassNotFoundException`. Bản Docker không bị ảnh hưởng.

## 🏗️ Kiến trúc backend

Clean Architecture 4 lớp trong `backend/src/main/java/com/oism`:

| Lớp | Nội dung |
| --- | --- |
| `domain` | Entity, repository, `ErrorCode`, `BusinessException` |
| `application` | Use case/service (`AuthService`), command, result |
| `infrastructure` | JWT, Security, tenant filter, OpenAPI |
| `web` | Controller, DTO, `GlobalExceptionHandler` |

Lỗi trả về thống nhất dạng `{"errorCode": "IV40901", "message": "...", "details": []}` với mã `<TIỀN TỐ><HTTP><SỐ>` (AU, PR, IV, OD, RP, SY).

## 📁 Cấu trúc thư mục

```text
.
├── README.md
├── docker-compose.yml         # PostgreSQL + backend (+ 2 frontend, profile "frontend")
├── backend/                   # Spring Boot 3 (Dockerfile, pom.xml, src/)
├── frontend-admin/            # Admin Dashboard (React + Vite + TS + Ant Design)
├── frontend-pos/              # POS (React + Vite + TS, PWA)
├── docs/                      # Kế hoạch, hướng dẫn, tài liệu từng sprint, sơ đồ thiết kế
└── .github/                   # Mẫu pull request, workflow CI
```

## 🗓️ Lộ trình

| Sprint | Mục tiêu |
| --- | --- |
| 1 | Phân tích, thiết kế, nền tảng backend, đăng ký/đăng nhập JWT, Swagger ✅ |
| 2 | Sản phẩm, sổ cái tồn kho, nhập kho, giá vốn |
| 3 | Đơn hàng, chống bán vượt, chuyển kho, kiểm kê, simulator |
| 4 | POS, realtime, tác vụ nền, báo cáo ✅ |
| 5 | Kiểm thử, hoàn thiện, Docker Compose + seed data, CI/CD, tài liệu |

## 🤝 Quy tắc làm việc

- Nhánh: `main`, `develop`, `feature/*`.
- Mỗi pull request cần 1 người duyệt và CI xanh (biên dịch + test) mới được merge.
- Thư mục `docs/internal/` là tài liệu nội bộ, không đẩy lên GitHub.
