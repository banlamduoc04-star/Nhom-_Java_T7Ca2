# Bàn giao phiên làm việc — Sprint 2 OISM

Dán nội dung file này (hoặc đưa đường dẫn) cho phiên làm việc tiếp theo. Cập nhật lúc dừng: 04/11/2026. Thư mục làm việc: thư mục gốc repo. Nhánh hiện tại: `feature/sprint2` (tách từ `feature/sprint1-foundation`).

## 1. Yêu cầu gốc của người dùng (nguyên ý)

1. Trong `docx/` bổ sung **hướng dẫn chạy dự án** và **file cấu hình/chuẩn chung** để các thành viên viết code đúng chuẩn.
2. Bắt đầu **Sprint 2**: đối chiếu nhiệm vụ Sprint 2 với code hiện tại và **làm thay** nhiệm vụ của các task đó (không chỉ lập kế hoạch).
3. Tái cấu trúc thư mục dự án cho chuyên nghiệp, mặc định mọi thứ theo **Kế hoạch dự án OISM** (`docx/Kế hoạch dự án OISM – 5 Sprint, 7 thành viên.md`).
4. **Đổi công nghệ frontend theo kế hoạch: React + Vite + TypeScript** (bỏ Next.js).
5. Tạo nhánh mới (đã tạo `feature/sprint2`), **comment từng task Jira của các thành viên rồi chuyển Done**, **`git push`** lên; người dùng sẽ tự merge vào `develop`. Người dùng đã chọn: chuyển **Done hết**.
6. Cuối cùng trình bày kế hoạch.

**Chỉ thị mới nhất của người dùng (ghi đè phần còn lại):** dừng sau khi hoàn thành **tái cấu trúc** và **hướng dẫn chạy chung**, rồi viết file bàn giao này để phiên khác làm tiếp. Vì vậy: **chưa commit, chưa push, chưa đụng Jira.**

## 2. Đã làm xong (kèm bằng chứng)

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Tái cấu trúc thư mục | Xong | Bỏ lớp `omnichannel-oism/`; `backend/`, `frontend-admin/`, `frontend-pos/`, `docker-compose.yml` nằm ngay ở gốc (dùng `git mv`, giữ lịch sử). README, compose, `.gitignore` đã cập nhật |
| Hướng dẫn chạy | Xong | `docx/Hướng dẫn chạy dự án OISM.md` |
| Chuẩn cấu hình/code chung | Xong | `docx/Chuẩn cấu hình chung.md` |
| Backend Sprint 2 | Xong, đã kiểm chứng | `./mvnw clean verify` trong `backend/`: **EXIT 0, 62 unit test + 25 test tích hợp (PostgreSQL thật bằng Testcontainers), 0 lỗi** |
| Frontend Admin (React+Vite+antd) | Build sạch | `npm run build` OK; chạy thử trên trình duyệt với backend thật: đăng nhập, danh sách sản phẩm, phiếu nhập, tồn kho hiển thị đúng dữ liệu mẫu |
| Frontend POS (React+Vite, PWA manifest) | Build sạch | `npm run build` OK; `npm test` 6/6 (logic giỏ hàng). **Chưa chạy thử trên trình duyệt** |
| Smoke test API trên jar thật | Xong | Seed chạy; giá vốn POLO-S = 95.000 khớp tính tay; Cashier gọi API Owner → 403; OpenAPI có 34 path |

Chi tiết backend đã thêm (theo FR trong kế hoạch):

- **AUTH:** `BranchService/Controller` (FR-AUTH-04, mã lỗi mới `AU40902`), `UserService/Controller` (FR-AUTH-05), `@PreAuthorize` theo vai trò.
- **PROD (V2):** danh mục nhiều cấp, thương hiệu, sản phẩm + SKU nhiều biến thể một lần, EAN-13, đổi giá, tìm kiếm/lọc/phân trang, `GET /skus` (tìm SKU phẳng kèm `available`), `GET /skus/lookup`.
- **INV + COST (V3):** `InventoryLedgerService.post` (khóa dòng `FOR UPDATE`, `Propagation.MANDATORY`), trigger chặn UPDATE/DELETE sổ cái, nhà cung cấp, phiếu nhập Nháp→Xác nhận, giá vốn bình quân (`CostCalculator`), xem tồn và sổ cái. Bảng chuyển kho/kiểm kê đã tạo sẵn nhưng **chưa có code** (Sprint 3).
- **ORD (V4):** entity + `OrderStateMachine` thuần domain, `POST/GET /orders` (Draft). Giữ/trừ hàng chưa có (Sprint 3).
- **Hạ tầng:** `DemoDataSeeder` (bật bằng `OISM_SEED_ENABLED=true`), ArchUnit (6 luật), Testcontainers, JaCoCo (profile `coverage`), Failsafe, CI `.github/workflows/ci.yml`, Dockerfile nginx cho 2 frontend.

## 3. Chưa làm (việc của phiên tiếp theo)

Làm theo thứ tự:

1. **Rà soát nhanh rồi commit.** Cây làm việc đang có ~220 thay đổi **chưa commit**. Chia commit theo chủ đề, tiếng Việt, kiểu Conventional Commits (xem Chuẩn cấu hình chung, mục 8), ví dụ:
   `chore: tái cấu trúc thư mục, đưa backend/frontend ra gốc repo` → `docs: hướng dẫn chạy và chuẩn cấu hình chung` → `feat(auth): API chi nhánh và nhân viên` → `feat(prod): ...` → `feat(inv): ...` → `feat(ord): ...` → `test: ...` → `feat(admin): ...` → `feat(pos): ...` → `chore(ci): ...`.
   Mỗi commit kết thúc bằng dòng `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. **Không** commit `docs/`, `target/`, `node_modules/`, `dist/`.
2. **Tài liệu kiểm thử còn thiếu cho JC-13** (đã có bản nháp tốt trong `docx/sprint2/`):
   - `Kế hoạch kiểm thử v1.md` và `Test case v1.md` (44 case) — đã viết; trong đó có 2 chỗ ghi "chưa viết".
   - **Chưa viết:** `RTM v2.md` (ánh xạ FR/NFR → thiết kế → mã → test cho Sprint 1–2; tên lớp/test đã liệt kê trong Test case v1) và báo cáo test thủ công API Sprint 1.
3. **`git push`** nhánh `feature/sprint2` (`git push -u origin feature/sprint2`). Không đụng `main`/`develop`; người dùng tự merge.
4. **Jira** (làm *sau khi* push, chỉ khi người dùng vẫn muốn): comment từng task rồi chuyển sang Done (xem mục 5). Người dùng đã đồng ý "Done hết", nhưng comment phải **ghi trung thực** phần chưa kiểm chứng (mục 4).
5. Báo cáo cuối cho người dùng: những gì đã làm, đã kiểm chứng, chưa kiểm chứng.

## 4. Điều chưa kiểm chứng / cần nói thẳng trong comment Jira

| Task | Chưa kiểm chứng |
|---|---|
| JC-14 (M2) | Workflow CI **chưa chạy trên GitHub** (chỉ viết); JaCoCo chỉ chạy được ở đường dẫn không dấu (`-Pcoverage`), chưa chạy lần nào trên máy này; image Docker frontend **chưa build thử** |
| JC-13 (M1) | Chưa có RTM v2 và báo cáo test thủ công; Postman chưa có |
| JC-18 (M6) | Chưa chạy tay: tạo sản phẩm nhiều biến thể, sinh mã vạch/in tem, tạo–sửa–xác nhận phiếu nhập trên giao diện (API đã có test tự động); chưa có màn đơn hàng, chuyển kho (thuộc Sprint 3) |
| JC-19 (M7) | Chưa chạy tay thao tác bàn phím F2/F9/Esc trên trình duyệt; thanh toán/in hóa đơn là Sprint 4; Service worker chưa có (Sprint 3) |
| JC-17 (M5) | Seed data đã chạy thật; "50 SKU" thực hiện trong tenant A |
| JC-15, JC-16 | Đã kiểm chứng bằng test tích hợp |
| Sprint 1 (đã Done trước đó) | JC-4 ghi "API chi nhánh & nhân viên" nhưng code lúc đó **chưa có** (nay đã bổ sung); JC-7 ghi "React+Vite" nhưng code là Next.js (nay đã đổi sang Vite) |

## 4b. Thông tin Jira

- Site: `https://hoangdungnguyen077.atlassian.net`, `cloudId` = `a3ed610b-46e4-47ee-bdb8-93f4d54e6098`.
- Epic Sprint 2: **JC-9** (To Do). Task Sprint 2:

| Key | Thành viên | Nội dung rút gọn |
|---|---|---|
| JC-13 | M1 Dung Hoang | Kế hoạch test v1, test case AUTH/PROD/INV, test API Sprint 1, RTM v2 |
| JC-14 | M2 Nguyễn Đức Thuận | Dockerfile + compose, GitHub Actions/JaCoCo, test tenant, ArchUnit |
| JC-15 | M3 Trần Ngọc Lâm | API PROD đầy đủ, EAN-13, giá, tìm kiếm |
| JC-16 | M4 Lê Quốc Khánh | InventoryLedgerService, NCC, phiếu nhập, avg_cost |
| JC-17 | M5 Trương Quang Vinh | Entity đơn hàng, OrderStateMachine, API tạo đơn Draft, seed |
| JC-18 | M6 Phạm Chấn Nguyên | Admin: danh mục, thương hiệu, sản phẩm, phiếu nhập, tồn kho |
| JC-19 | M7 Nghĩa Chu | POS: bán hàng, giỏ hàng, quét mã, F2/F9/Esc |

- Dùng `getJiraIssue` để đọc mô tả đầy đủ từng task trước khi comment; dùng `addOrEditJiraIssueComment` rồi `transitionJiraIssue` (lấy transition "Done" qua `getTransitionsForJiraIssue` qua `discover`/`executeRead` nếu cần). Comment/chuyển trạng thái là hành động gửi ra ngoài: chỉ làm khi người dùng vẫn đồng ý.

## 5. Quy tắc và ngữ cảnh cần nhớ

- Ngôn ngữ: **tiếng Việt có dấu** cho tài liệu, message API, giao diện. Code/comment theo Chuẩn cấu hình chung.
- Kế hoạch là nguồn sự thật. Đã thêm mã lỗi `AU40902` (409, không ẩn được chi nhánh còn tồn/đơn chưa kết thúc) — **chưa cập nhật bảng mã lỗi trong kế hoạch** (`docx/Kế hoạch dự án OISM….md` và `.docx`); nên bổ sung.
- Sổ cái tồn kho chỉ ghi qua `InventoryLedgerService`; mọi truy vấn có `tenantId` (không dùng `findById`).
- Tên sản phẩm phải khớp `^[A-Z][a-zA-Z0-9\s]{2,50}$` (không dấu) — theo đề; dữ liệu seed đã tuân thủ.
- Người dùng đang chạy sẵn stack Docker cũ (`oism_postgres` cổng 5432, `oism_backend` cổng 8080, container cố định tên). **Không** `docker compose up` trong repo mà không báo, kẻo đụng tên container/cổng. Khi cần chạy thử, dùng cổng riêng (ví dụ Postgres 55432 + backend `SERVER_PORT=8081`) rồi tắt.
- Đường dẫn repo của người dùng có dấu (`Cuối Khóa JAVA`): JaCoCo (`-Pcoverage`) lỗi, `spring-boot:run` có thể lỗi; `./mvnw clean verify` và `java -jar` vẫn chạy được.
- Máy có JDK 25, Node 24, Docker 29. Test tích hợp cần `api.version=1.44` và Testcontainers 1.21.3 (đã cấu hình trong `pom.xml`).
- Mật khẩu seed `Oism@12345` chỉ dành cho dev/demo; `OISM_SEED_ENABLED` mặc định `false`.

## 6. Lệnh kiểm tra nhanh

```bash
cd backend && ./mvnw clean verify                 # kỳ vọng: BUILD OK, 62 unit + 25 tích hợp
cd frontend-admin && npm install && npm run build
cd frontend-pos && npm install && npm run build && npm test
git status --short | head                         # xem cây làm việc trước khi commit
```

Chạy thử với dữ liệu mẫu (cổng riêng, không đụng stack đang chạy):

```bash
docker run -d --name oism_smoke_pg -e POSTGRES_DB=oism_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 55432:5432 postgres:16-alpine
cd backend && ./mvnw -q -DskipTests package
OISM_DB_URL=jdbc:postgresql://localhost:55432/oism_db OISM_SEED_ENABLED=true OISM_CORS_ORIGINS=http://localhost:5173 SERVER_PORT=8081 java -jar target/backend-0.1.0-SNAPSHOT.jar
cd frontend-admin && VITE_API_URL=http://localhost:8081/api/v1 npx vite --port 5173   # đăng nhập owner.a@oism.vn / Oism@12345
# xong: dừng 2 tiến trình trên và `docker rm -f oism_smoke_pg`
```

## 7. Vấn đề nhỏ đã biết (không chặn)

- Backend log cảnh báo `UserDetailsServiceAutoConfiguration` (sinh mật khẩu mặc định, không dùng) — có thể loại trừ auto-config đó.
- `oxlint` ở frontend-admin có cảnh báo `set-state-in-effect` (không phải lỗi); bundle Admin ~1,4 MB (chưa tách chunk).
- Chưa có Postman collection, chưa có màn quản lý chi nhánh/nhân viên trên Admin (thuộc Sprint 4 của M6).
- Hai file `.docx` (kế hoạch, sơ đồ) **không** được cập nhật; chỉ bản `.md` của kế hoạch là bản chuẩn trong repo.
