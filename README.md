
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
| Kiểm thử | JUnit 5, Mockito, Testcontainers, ArchUnit, JaCoCo, Vitest, Postman/newman, Playwright |
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

Backend là **Spring Boot MVC dạng REST** (`@RestController`), tổ chức theo **Clean Architecture 4 lớp** (Kế hoạch mục 4, quyết định 1). Mã được chia **theo lớp kiến trúc trước, theo module nghiệp vụ sau**, chứ không theo kiểu quen thuộc `controller/ service/ repository/ model/`. Cách chia này cho phép ArchUnit kiểm tra quy tắc phụ thuộc trong CI (domain không được biết web hay hạ tầng; controller không gọi repository). Phần "View" của MVC là hai ứng dụng React.

```text
backend/src/main/java/com/oism/
├── domain/                      # Lõi nghiệp vụ, không phụ thuộc lớp nào khác
│   ├── model/                   #   Entity JPA: SalesOrder, Sku, StockBalance, InventoryTransaction…
│   ├── repository/              #   Interface Spring Data (mọi truy vấn có tenantId), SalesAnalyticsRepository
│   ├── service/                 #   Quy tắc thuần: OrderStateMachine, CostCalculator, Ean13
│   ├── exception/               #   ErrorCode (AU/PR/IV/OD/RP/SY), BusinessException
│   └── event/                   #   OrderCreatedEvent, StockAlertEvent
├── application/                 # Use case (@Service, @Transactional) + record …View trả ra ngoài
│   ├── auth/ branch/ user/ tenant/ common/   # common: PageResult, Paging, Sha256
│   ├── catalog/                 #   sản phẩm, SKU, danh mục, thương hiệu (PROD)
│   ├── inventory/               #   sổ cái, nhập kho, chuyển kho, kiểm kê, cảnh báo (INV, COST)
│   ├── order/                   #   đơn hàng, giữ hàng, POS, webhook key (ORD, RSE, POS, SIM)
│   └── report/                  #   lợi nhuận, báo cáo tồn, chốt ngày (REP)
├── infrastructure/              # Kỹ thuật: được phụ thuộc domain + application
│   ├── security/                #   JWT, SecurityConfig, WebhookApiKeyFilter
│   ├── tenant/                  #   TenantContext, bật Hibernate @Filter theo request
│   ├── websocket/               #   STOMP, chặn kênh khác tenant
│   ├── scheduler/               #   hủy đơn giữ quá hạn, chốt sổ ngày
│   ├── persistence/             #   truy vấn tổng hợp báo cáo (JPQL SUM/GROUP BY)
│   └── seed/ config/            #   dữ liệu mẫu, OpenAPI
└── web/                         # REST: controller + request DTO, chỉ gọi application
    ├── auth/ branch/ user/ tenant/ catalog/ inventory/ order/ report/ common/
    └── error/                   #   GlobalExceptionHandler → {errorCode, message, details}
```

| Lớp | Được phép phụ thuộc vào |
| --- | --- |
| `web` | `application` (không gọi `domain.repository` trực tiếp) |
| `application` | `domain` |
| `infrastructure` | `domain`, `application` |
| `domain` | không lớp nào |

Lỗi trả về thống nhất dạng `{"errorCode": "IV40901", "message": "...", "details": []}` với mã `<TIỀN TỐ><HTTP><SỐ>` (AU, PR, IV, OD, RP, SY). Quy ước chi tiết: [`docs/huong-dan/chuan-cau-hinh-chung.md`](docs/huong-dan/chuan-cau-hinh-chung.md).

## 🧪 Kiểm thử

| Loại | Lệnh | Kết quả gần nhất (11/11/2026) |
| --- | --- | --- |
| Unit + ArchUnit | `cd backend && ./mvnw test` | 115/115 |
| Tích hợp (cần Docker) | `cd backend && ./mvnw verify` | 59/59 |
| Coverage + cổng 80% | `cd backend && ./mvnw -Pcoverage verify` (đường dẫn không dấu) | sổ cái 95,8%, giữ hàng 84,1%, giá vốn 100% |
| Postman (bản Docker) | `npx newman run postman/<collection>.json -e postman/OISM_Local.postman_environment.json` | 244/244 assertion |
| E2E (bản Docker) | `cd e2e && npm ci && npx playwright install chromium && npm test` | 7/7 |

Kết quả đầy đủ, hiệu năng và RTM: [`docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md`](docs/sprint5/ket-qua-kiem-thu-giai-doan-4.md), [`docs/sprint5/rtm-v5.md`](docs/sprint5/rtm-v5.md).

## 📁 Cấu trúc thư mục

```text
.
├── README.md
├── docker-compose.yml         # PostgreSQL + backend (+ 2 frontend, profile "frontend")
├── backend/                   # Spring Boot 3 (Dockerfile, pom.xml, src/)
├── frontend-admin/            # Admin Dashboard (React + Vite + TS + Ant Design)
├── frontend-pos/              # POS (React + Vite + TS, PWA)
├── postman/                   # Collection + environment Postman (chạy bằng newman)
├── e2e/                       # Kiểm thử E2E Playwright (Admin + POS)
├── docs/                      # Kế hoạch, hướng dẫn, tài liệu từng sprint, sơ đồ thiết kế
└── .github/                   # Mẫu pull request, workflow CI
```

## 🗓️ Lộ trình

| Sprint | Mục tiêu |
| --- | --- |
| 1 | Phân tích, thiết kế, nền tảng backend, đăng ký/đăng nhập JWT, Swagger ✅ |
| 2 | Sản phẩm, sổ cái tồn kho, nhập kho, giá vốn ✅ |
| 3 | Đơn hàng, chống bán vượt, chuyển kho, kiểm kê, simulator ✅ |
| 4 | POS, realtime, tác vụ nền, báo cáo ✅ |
| 5 | Kiểm thử, hoàn thiện, Docker Compose + seed data, CI/CD, tài liệu (đang làm – xem [kế hoạch sửa lỗi](docs/sprint5/ke-hoach-sua-loi-sprint5.md)) |

