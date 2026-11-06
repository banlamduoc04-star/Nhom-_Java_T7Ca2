# Hướng dẫn chạy dự án OISM

Tài liệu dành cho 7 thành viên (M1–M7). Làm theo đúng thứ tự; nếu gặp lỗi, xem mục **Lỗi thường gặp** ở cuối.

## 1. Chuẩn bị máy

| Công cụ | Phiên bản | Dùng để |
|---|---|---|
| Git | mới nhất | Quản lý mã nguồn |
| Docker Desktop | mới nhất | Chạy PostgreSQL, chạy cả hệ thống, chạy test tích hợp |
| JDK | **17** (kế hoạch yêu cầu 17; JDK 21/25 cũng chạy được) | Phát triển backend (M2–M5) |
| Node.js | **22 trở lên** | Phát triển Admin và POS (M6, M7) |
| IDE | IntelliJ IDEA hoặc VS Code | Tùy chọn |

Maven **không cần cài**: dự án có sẵn `mvnw` (Maven Wrapper).

> **Quan trọng:** đặt thư mục dự án ở đường dẫn **không dấu, không khoảng trắng**, ví dụ `C:\dev\oism`. Đường dẫn như `...\Cuối Khóa JAVA\...` làm `spring-boot:run` và JaCoCo lỗi. Bản chạy bằng Docker không bị ảnh hưởng.

## 2. Lấy mã nguồn

```bash
git clone https://github.com/<tổ-chức>/<repo>.git oism
cd oism
git checkout develop
git checkout -b feature/<module>-<mô-tả-ngắn>     # ví dụ: feature/prod-category-api
```

Cấu trúc thư mục:

```text
oism/
├── backend/            Spring Boot 3 (Java 17)
├── frontend-admin/     Admin Dashboard (React + Vite + TypeScript + Ant Design)
├── frontend-pos/       POS (React + Vite + TypeScript, PWA)
├── docker-compose.yml  PostgreSQL + backend (+ 2 frontend, profile "frontend")
├── docs/               Kế hoạch, hướng dẫn, tài liệu từng sprint, sơ đồ
└── .github/            CI và mẫu pull request
```

## 3. Cách 1 — Chạy cả hệ thống bằng Docker (đơn giản nhất)

Không cần cài Java, Maven hay Node.

```bash
docker compose up -d --build
```

| Dịch vụ | Địa chỉ |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API | http://localhost:8080/api/v1 |
| PostgreSQL | `localhost:5432`, DB `oism_db`, user `postgres`, mật khẩu `postgres` |

**Chạy kèm Admin và POS:**

```bash
docker compose --profile frontend up -d --build
```

Admin: http://localhost:3000 — POS: http://localhost:3001.

**Nạp dữ liệu mẫu để demo** (2 cửa hàng, 3 chi nhánh, 50 SKU, 3 nhà cung cấp, phiếu nhập đã xác nhận):

```bash
# Linux/macOS/Git Bash
OISM_SEED_ENABLED=true docker compose up -d --build
```

```powershell
# PowerShell
$env:OISM_SEED_ENABLED="true"; docker compose up -d --build
```

Tài khoản mẫu (mật khẩu chung `Oism@12345`, **chỉ dùng cho dev/demo**):

| Cửa hàng | Vai trò | Email |
|---|---|---|
| Demo Shop A | Owner | `owner.a@oism.vn` |
| Demo Shop A | Staff | `staff.a@oism.vn` |
| Demo Shop A | Cashier | `cashier.a@oism.vn` |
| Demo Shop B | Owner | `owner.b@oism.vn` |
| Demo Shop B | Cashier | `cashier.b@oism.vn` |

Lệnh hay dùng:

```bash
docker compose logs -f backend          # xem log backend
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose down                     # dừng, giữ dữ liệu
docker compose down -v                  # dừng và XÓA sạch dữ liệu
```

## 4. Cách 2 — Phát triển hằng ngày (IDE)

### 4.1. Cơ sở dữ liệu

```bash
docker compose up -d postgres
```

### 4.2. Backend (M2–M5)

```bash
cd backend
./mvnw spring-boot:run                  # Windows CMD/PowerShell: .\mvnw.cmd spring-boot:run
```

- Chạy profile `dev`, cổng 8080, tự chạy Flyway tạo bảng (thư mục `src/main/resources/db/migration`).
- Muốn có dữ liệu mẫu: thêm biến môi trường `OISM_SEED_ENABLED=true`.
- Cấu hình đọc từ biến môi trường (có mặc định cho dev), xem [Chuẩn cấu hình chung](chuan-cau-hinh-chung.md#6-biến-môi-trường).

Kiểm thử:

```bash
./mvnw test                             # unit test + ArchUnit, vài giây, không cần Docker
./mvnw clean verify                     # thêm test tích hợp (cần Docker chạy), khoảng 1–2 phút
./mvnw -Pcoverage clean verify          # thêm báo cáo coverage: target/site/jacoco/index.html (cần đường dẫn không dấu)
```

### 4.3. Admin (M6)

```bash
cd frontend-admin
npm install
npm run dev                             # http://localhost:5173
```

### 4.4. POS (M7)

```bash
cd frontend-pos
npm install
npm run dev                             # cổng kế tiếp (5174 nếu 5173 đang bận)
npm test                                # unit test giỏ hàng
```

POS chạy được **không cần backend** bằng dữ liệu giả: tạo file `frontend-pos/.env.local` với nội dung `VITE_USE_MOCK=true`, bấm Đăng nhập (không cần mật khẩu). Mã vạch mẫu: `8930000000012`, `8930000000029`.

Trỏ frontend tới backend khác: đặt `VITE_API_URL` (mặc định `http://localhost:8080/api/v1`) trong `.env.local`. Backend chỉ cho phép CORS từ các địa chỉ trong `OISM_CORS_ORIGINS` (mặc định `http://localhost:5173`; trong `docker-compose.yml` có thêm 3000, 3001).

## 5. Thử nhanh API bằng Swagger

1. Mở http://localhost:8080/swagger-ui.html.
2. `POST /auth/register` tạo cửa hàng mới (hoặc dùng tài khoản mẫu ở trên).
3. `POST /auth/login` → copy `accessToken`.
4. Bấm **Authorize** (góc phải), dán token (không gõ chữ `Bearer`).
5. Thử `GET /me`, `POST /categories`, `POST /products`, `POST /purchase-receipts`…

Lỗi luôn có dạng `{"errorCode": "IV40901", "message": "...", "details": []}`.

## 6. Quy trình git hằng ngày

```bash
git checkout develop && git pull
git checkout -b feature/<module>-<mô-tả>        # tách nhánh từ develop
# ... code, chạy test ...
git add <file>                                  # không dùng "git add ." khi chưa xem git status
git commit -m "feat(prod): thêm API danh mục"   # xem Chuẩn cấu hình chung, mục 8
git push -u origin feature/<module>-<mô-tả>
```

Mở pull request vào `develop`, điền mẫu PR. Chỉ merge khi **CI xanh** và **có 1 người duyệt**.

## 7. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `ClassNotFoundException` khi `spring-boot:run` | Đường dẫn có dấu/khoảng trắng | Chuyển repo sang `C:\dev\oism` |
| `Connection refused localhost:5432` | PostgreSQL chưa chạy | `docker compose up -d postgres` |
| `Port 8080 already in use` | Có backend/container khác đang chạy | `docker compose down` hoặc đổi `SERVER_PORT=8081` |
| `oism.jwt.secret phải có ít nhất 32 byte` | Thiếu biến `OISM_JWT_SECRET` ở profile không phải dev | Đặt biến này (≥ 32 ký tự) |
| `Could not find a valid Docker environment` khi chạy test tích hợp | Docker chưa bật, hoặc Docker Engine mới từ chối API cũ | Bật Docker Desktop; `pom.xml` đã ép `api.version=1.44` cho Failsafe |
| Test tích hợp báo `Status 400` khi gọi Docker | Như trên | Cập nhật Testcontainers (đã ghim 1.21.3) |
| `java.lang.NoSuchMethodError` / lỗi lạ khi build | Cache Maven hỏng hoặc sai JDK | `./mvnw clean`; kiểm tra `java -version` |
| Flyway báo `checksum mismatch` | Đã sửa file migration đã chạy | **Không sửa migration cũ**; tạo `V{n+1}__...sql` mới. Môi trường dev: `docker compose down -v` |
| Trình duyệt báo CORS | Địa chỉ frontend chưa có trong `OISM_CORS_ORIGINS` | Thêm địa chỉ đó rồi chạy lại backend |
| Admin đăng nhập xong bị đá về `/login` | Token hết hạn và refresh thất bại | Đăng nhập lại; kiểm tra giờ máy |
| Cashier đăng nhập Admin thấy "dùng ứng dụng POS" | Đúng thiết kế (FR-AUTH-03) | Dùng POS |

## 8. Việc cần làm trước khi tạo pull request

- [ ] `./mvnw clean verify` xanh (backend) / `npm run build` và `npm test` xanh (frontend).
- [ ] Migration mới đặt đúng số thứ tự, không sửa migration cũ.
- [ ] Swagger có mô tả (`@Operation`) và ràng buộc validation cho endpoint mới.
- [ ] Không commit `.env`, `target/`, `node_modules/`, `dist/`, mật khẩu, khóa bí mật.
- [ ] Cập nhật README/tài liệu nếu đổi cách chạy; cập nhật RTM nếu thêm yêu cầu.
