# Báo cáo test thủ công API Sprint 1

**Ngày test:** 04/11/2026
**Người thực hiện:** Claude (chạy `curl` theo yêu cầu của nhóm)
**Phạm vi:** FR-AUTH-01, FR-AUTH-02, FR-AUTH-03 và tiêu chí nghiệm thu Sprint 1 trong *Kế hoạch dự án OISM* (đăng ký, đăng nhập, `GET /me`, Cashier gọi API Owner nhận 403 `AU40301`).

## 1. Môi trường

| Mục | Giá trị |
|---|---|
| Bản build | `backend/target/backend-0.1.0-SNAPSHOT.jar` (nhánh `feature/sprint2`) |
| Backend | Chạy bằng `java -jar`, cổng `8081`, `OISM_SEED_ENABLED=true`, log "Started BackendApplication" |
| CSDL | PostgreSQL 16 riêng (`oism_smoke_pg`, cổng `55432`), không dùng stack Docker đang chạy của người dùng (5432/8080) |
| Công cụ | `curl`, base URL `http://localhost:8081/api/v1` |
| Dữ liệu | Seed dev: `owner.a@oism.vn`, `staff.a@oism.vn`, `cashier.a@oism.vn`, `owner.b@oism.vn` (mật khẩu dev, không ghi trong báo cáo); tài khoản đăng ký mới `smoke.owner.b@example.com` |

Quy ước: `<token>` thay cho mọi access/refresh token. "Đạt" chỉ khi kết quả thực nhận khớp kế hoạch.

## 2. Kết quả từng ca

| STT | Ca test | Request | Kết quả mong đợi (theo kế hoạch) | Kết quả thực nhận | KQ |
|---|---|---|---|---|---|
| 1 | Đăng ký hợp lệ | `POST /auth/register` `{storeName, fullName, email, phone, password}` | 201; tạo tenant + Owner + chi nhánh mặc định | 201, trả `tenantId`, `userId`, `branchId`, email chuẩn hóa chữ thường | Đạt |
| 2 | Đăng ký trùng email (khác hoa/thường) | `POST /auth/register` với `smoke.owner.b@EXAMPLE.com` | 409 `AU40901` | 409 `AU40901` "Email đã được đăng ký" | Đạt |
| 3 | Đăng ký body sai (tên cửa hàng rỗng, email sai, mật khẩu 3 ký tự) | `POST /auth/register` | 400 `AU40001` | 400 `AU40001`, `details` liệt kê 3 trường `storeName`, `email`, `password` | Đạt |
| 4 | Đăng nhập bằng email | `POST /auth/login` `{identifier: email, password}` | 200; access 60 phút, có refresh token | 200; `accessExpiresIn=3600`, `refreshExpiresIn=604800`, `tokenType=Bearer` | Đạt |
| 5 | Đăng nhập bằng số điện thoại | `POST /auth/login` `{identifier: "0911222333", password}` | 200 | 200, có `<token>` | Đạt |
| 6 | Sai mật khẩu | `POST /auth/login` | 401 `AU40101` | 401 `AU40101` "Sai thông tin đăng nhập" | Đạt |
| 7 | Email không tồn tại | `POST /auth/login` | 401 `AU40101` (cùng mã, không lộ tài khoản) | 401 `AU40101`, cùng thông báo như ca 6 | Đạt |
| 8 | `GET /me` có token | `GET /me` + `Authorization: Bearer <token>` | 200 thông tin người dùng | 200, trả `userId`, `tenantId`, `role=OWNER`, `branchId` | Đạt |
| 9 | `GET /me` không token | `GET /me` | 401 `AU40101` | 401 `AU40101` (JSON) | Đạt |
| 10 | `GET /me` token sai | `Bearer abc.def.ghi` | 401 `AU40101` | 401 `AU40101` | Đạt |
| 11 | Làm mới token | `POST /auth/refresh` `{refreshToken: <token>}` | 200; token mới | 200; refresh token mới khác token cũ | Đạt |
| 12 | Dùng lại refresh token cũ | `POST /auth/refresh` với token đã xoay | 401 `AU40101` | 401 `AU40101` "Refresh token không hợp lệ hoặc đã hết hạn" | Đạt |
| 13 | Sau ca 12, dùng refresh token mới | `POST /auth/refresh` với token mới của ca 11 | 401 `AU40101` (thu hồi mọi token khi phát hiện tái sử dụng, theo Test case v1 TC-AUTH-08) | 401 `AU40101` | Đạt |
| 14 | Refresh token không tồn tại | `POST /auth/refresh` `{refreshToken:"nonexistent"}` | 401 `AU40101` | 401 `AU40101` | Đạt |
| 15 | Đăng xuất lần 1 và lần 2 | `POST /auth/logout` + Bearer + `{refreshToken}` (2 lần) | 204 (idempotent) | 204 cả hai lần | Đạt |
| 16 | Refresh sau khi đăng xuất | `POST /auth/refresh` với token vừa thu hồi | 401 `AU40101` | 401 `AU40101` | Đạt |
| 17 | Đăng xuất không token | `POST /auth/logout` không Bearer | 401 `AU40101` | 401 `AU40101` | Đạt |
| 18 | Cashier gọi API chỉ Owner: `GET /users` | `GET /users` + token `cashier.a` | 403 `AU40301` | 403 `AU40301` "Không đủ quyền thực hiện thao tác này" | Đạt |
| 19 | Cashier `POST /branches` | token `cashier.a` | 403 `AU40301` | 403 `AU40301` | Đạt |
| 20 | Cashier `POST /categories` | token `cashier.a` | 403 `AU40301` | 403 `AU40301` | Đạt |
| 21 | Gọi `GET /users` không token | không Bearer | 401 `AU40101` | 401 `AU40101` | Đạt |
| 22 | Owner xem `GET /users` | token `owner.a` | 200 | 200, danh sách 3 tài khoản của tenant A (không thấy tenant B) | Đạt |
| 23 | Owner khóa Cashier | `PATCH /users/{id}/status` `{active:false}` | 200 | 200, `active=false` | Đạt |
| 24 | Đăng nhập tài khoản bị khóa | `POST /auth/login` (cashier.a) | 403 `AU40302` | 403 `AU40302` "Tài khoản đã bị khóa" | Đạt |
| 25 | Owner mở khóa, đăng nhập lại | `PATCH ... {active:true}` rồi `POST /auth/login` | 200 và 200 | 200 và 200 | Đạt |
| 26 | Đăng ký trùng số điện thoại | `POST /auth/register` với SĐT đã có | Kế hoạch chỉ nêu "email duy nhất" nên không có kỳ vọng rõ | 409 `AU40901` "Số điện thoại đã được đăng ký" | Xem lỗi L-01 |

**Tổng kết:** 25 ca Đạt, 0 ca Không đạt; 1 ca (26) là hành vi ngoài kế hoạch, ghi nhận dưới dạng quan sát. Có thêm 1 quan sát về token đã cấp của tài khoản bị khóa (L-02).

Ghi chú về quá trình chạy: lần chạy đầu, ca đăng ký dùng SĐT trùng với tài khoản seed `owner.a` (`0900000001`) nên trả 409 `AU40901`, và ca "trùng email" tiếp sau đó lại thành đăng ký hợp lệ (201). Đó là lỗi chọn dữ liệu test của người chạy, không phải lỗi hệ thống; các ca 1-3 và 26 đã được chạy lại với SĐT khác và ghi theo lần chạy lại.

## 3. Lỗi tìm thấy

Không có ca nào lệch so với kế hoạch ở mức Không đạt. Hai điểm dưới đây là quan sát cần nhóm xem xét, chưa tạo GitHub Issue (cần hỏi người dùng trước).

| Mã | Mô tả | Bước tái hiện | Thực nhận | Mong đợi / đề xuất | Mức |
|---|---|---|---|---|---|
| L-01 | Số điện thoại cũng bị ràng buộc duy nhất toàn hệ thống và trả `AU40901`. Kế hoạch chỉ ghi email duy nhất; bảng mã lỗi mô tả `AU40901` là "Email hoặc tên chi nhánh đã tồn tại", chưa nhắc số điện thoại. | 1. Đăng ký cửa hàng A với SĐT `0911222333`. 2. Đăng ký cửa hàng khác, email khác, cùng SĐT. | 409 `AU40901` "Số điện thoại đã được đăng ký" | Hành vi hợp lý vì đăng nhập bằng SĐT cần định danh duy nhất; đề xuất cập nhật kế hoạch/bảng mã lỗi cho khớp | Thấp (lệch tài liệu) |
| L-02 | Access token đã cấp vẫn dùng được sau khi tài khoản bị khóa, cho đến khi hết hạn (tối đa 60 phút). | 1. Đăng nhập `cashier.a`, giữ access token. 2. Owner `PATCH /users/{id}/status` `{active:false}`. 3. `GET /me` với token cũ. | 200 (vẫn trả thông tin) | Kế hoạch chỉ quy định khóa chặn đăng nhập (`AU40302`), nên chưa phải lỗi; nếu muốn khóa có hiệu lực ngay thì cần kiểm tra trạng thái khi xác thực. Chưa thử `refresh` sau khi khóa. | Thấp (cần nhóm quyết định) |

## 4. Kết luận

- Các API Sprint 1 (đăng ký, đăng nhập bằng email/SĐT, refresh xoay vòng, đăng xuất, `GET /me`, phân quyền Cashier/Owner, khóa tài khoản) hoạt động đúng kế hoạch trong 25/25 ca đã chạy; mã lỗi `AU40001`, `AU40101`, `AU40301`, `AU40302`, `AU40901` khớp bảng mã lỗi.
- Tiêu chí nghiệm thu Sprint 1 ("đăng nhập, dùng token gọi `GET /me` thành công; Cashier gọi API Owner nhận 403 `AU40301`") được đáp ứng ở mức API.
- Chưa kiểm tra bằng tay: đăng ký qua giao diện Admin, Swagger UI, token hết hạn thật (chỉ thử token sai), `refresh` sau khi khóa tài khoản.
- Môi trường thử đã dọn: dừng tiến trình java cổng 8081, xóa container `oism_smoke_pg`; `oism_postgres` và `oism_backend` của người dùng vẫn chạy.
