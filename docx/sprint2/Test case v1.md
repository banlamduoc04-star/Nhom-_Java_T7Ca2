# Test case v1 — AUTH, PROD, INV, ORD (Sprint 2)

44 test case; mỗi yêu cầu chức năng có ít nhất một case đúng (Đ) và một case lỗi (L). Cột **Tự động** trỏ tới test chạy cục bộ bằng `./mvnw clean verify`; CI chưa chạy trên GitHub (`U` = unit, `IT` = tích hợp trên PostgreSQL thật). Kết quả ghi theo lần chạy `./mvnw clean verify` ngày 04/11/2026 (xem cuối tài liệu).

Quy ước dữ liệu: "Owner/Staff/Cashier" là tài khoản của cùng một cửa hàng; "tenant B" là cửa hàng khác.

## AUTH

| Mã | FR | Loại | Mô tả và bước thực hiện | Kết quả mong đợi | Tự động |
|---|---|---|---|---|---|
| TC-AUTH-01 | FR-AUTH-01 | Đ | `POST /auth/register` với dữ liệu hợp lệ | 201; tạo tenant, chi nhánh "Chi nhánh chính", Owner; mật khẩu lưu BCrypt | U `AuthServiceTest.registerCreatesTenantBranchAndOwner`; IT `AuthFlowIT.registerLoginMeRefreshAndLogout` |
| TC-AUTH-02 | FR-AUTH-01 | L | Đăng ký với email đã có (khác hoa/thường) | 409 `AU40901`, không tạo tenant | U `registerRejectsDuplicateEmailWithAU40901`; IT `AuthFlowIT.rejectsBadCredentials...` |
| TC-AUTH-03 | FR-AUTH-01 | L | Đăng ký với tên cửa hàng rỗng, email sai định dạng, mật khẩu < 8 ký tự | 400 `AU40001`, `details` có 3 trường sai | IT `AuthFlowIT.rejectsBadCredentials...` |
| TC-AUTH-04 | FR-AUTH-02 | Đ | Đăng nhập bằng email, rồi bằng số điện thoại | 200; access token 3600 s, refresh 7 ngày; DB chỉ lưu băm refresh | U `loginByPhoneIssuesTokensAndStoresOnlyRefreshHash`; IT `AuthFlowIT.registerLogin...` |
| TC-AUTH-05 | FR-AUTH-02 | L | Đăng nhập sai mật khẩu; đăng nhập email không tồn tại | 401 `AU40101` (cùng mã, không lộ email có tồn tại hay không) | U `loginWithWrongPassword...`, `loginUnknownUser...`; IT `AuthFlowIT.rejectsBadCredentials...` |
| TC-AUTH-06 | FR-AUTH-02 | L | Đăng nhập tài khoản bị khóa | 403 `AU40302` | U `loginLockedAccountFailsWithAU40302`; IT `RbacIT.ownerManagesBranchesAndEmployees` |
| TC-AUTH-07 | FR-AUTH-02 | Đ | `POST /auth/refresh` với refresh hợp lệ | 200; token mới khác token cũ; token cũ bị thu hồi | U `refreshRotatesTokenAndRevokesOldOne`; IT `AuthFlowIT.registerLogin...` |
| TC-AUTH-08 | FR-AUTH-02 | L | Dùng lại refresh đã thu hồi; refresh hết hạn/không tồn tại | 401 `AU40101`; trường hợp dùng lại thì thu hồi mọi token của người dùng | U `reusingRevokedRefreshToken...`, `refreshRejectsExpiredAndUnknownTokens`; IT `AuthFlowIT.registerLogin...` |
| TC-AUTH-09 | FR-AUTH-02 | Đ | `POST /auth/logout` hai lần liên tiếp, rồi refresh bằng token đó | 204 cả hai lần (idempotent); refresh sau đó 401 | IT `AuthFlowIT.registerLogin...` |
| TC-AUTH-10 | FR-AUTH-02 | L | Gọi API không có token / token sai | 401 `AU40101` dạng JSON | IT `RbacIT.anonymousGets401AndWrongRoleGets403`; `AuthFlowIT.rejectsBadCredentials...` |
| TC-AUTH-11 | FR-AUTH-03 | L | Cashier gọi `POST /categories`, `POST /branches`, `GET /users`, `GET /purchase-receipts`, `GET /inventory-transactions` | 403 `AU40301` | IT `RbacIT.anonymousGets401AndWrongRoleGets403` |
| TC-AUTH-12 | FR-AUTH-03 | Đ | Staff xem phiếu nhập/sổ cái; mọi vai trò xem danh mục, chi nhánh, sản phẩm, tồn kho, `/me` | 200; Staff tạo danh mục → 403 | IT `RbacIT.anonymousGets401AndWrongRoleGets403` |
| TC-AUTH-13 | FR-AUTH-04 | Đ/L | Owner tạo chi nhánh, tạo lại cùng tên (khác hoa/thường), sửa tên | 201; lần 2 → 409 `AU40901`; sửa 200 | IT `RbacIT.ownerManagesBranchesAndEmployees` |
| TC-AUTH-14 | FR-AUTH-04 | L | Ẩn chi nhánh còn đơn chưa kết thúc; ẩn chi nhánh còn tồn kho | 409 `AU40902`; chi nhánh trống ẩn/hiện được | IT `RbacIT.cannotHideBranchWithStockOrOpenOrders` |
| TC-AUTH-15 | FR-AUTH-05 | Đ/L | Owner tạo Cashier có chi nhánh; tạo Cashier không chi nhánh; tạo vai trò Owner; khóa/mở khóa; đặt lại mật khẩu | 201; 400 `AU40001`; 400 `AU40001`; khóa → đăng nhập 403 `AU40302`, mở khóa → được; mật khẩu mới dùng được | IT `RbacIT.ownerManagesBranchesAndEmployees` |
| TC-AUTH-16 | FR-AUTH-05 | L | Owner khóa chính mình | 403 `AU40301` | IT `RbacIT.ownerManagesBranchesAndEmployees` |

## PROD

| Mã | FR | Loại | Mô tả và bước thực hiện | Kết quả mong đợi | Tự động |
|---|---|---|---|---|---|
| TC-PROD-01 | FR-PROD-02 | Đ | `POST /products` với `hasVariants=true` và 3 SKU | 201; 3 SKU; `GET /products/{id}` trả đủ, sắp theo mã SKU | IT `ProductApiIT.createsProductWithThreeVariantsInOneCall` |
| TC-PROD-02 | FR-PROD-02 | L | Tên `ao thun` (chữ thường đầu) | 400 `PR40001`, `details[0].field = name` | IT `ProductApiIT.validatesNameSkuPricesAndSingleProductRule` |
| TC-PROD-03 | FR-PROD-02/04 | L | Giá bán lẻ = 0 | 400 `PR40001` | IT như trên |
| TC-PROD-04 | FR-PROD-02 | L | Sản phẩm đơn (`hasVariants=false`) có 2 SKU; thêm SKU thứ hai vào sản phẩm đơn | 400 `PR40001` | IT như trên |
| TC-PROD-05 | FR-PROD-02 | L | Tạo SKU trùng mã; trùng mã vạch | 409 `PR40901` | IT `ProductApiIT.rejectsDuplicateSkuCodeAndBarcode` |
| TC-PROD-06 | FR-PROD-02 | Đ | Hai tenant cùng dùng mã SKU `CHUNG-01` | Cả hai thành công (duy nhất theo tenant) | IT `TenantIsolationIT.sameSkuCodeAndBarcodeMayExistInDifferentTenants` |
| TC-PROD-07 | FR-PROD-03 | Đ | `POST /skus/{id}/barcode` | 200; mã 13 số có check digit đúng; gọi lại → 409 `PR40901` | U `Ean13Test`; IT `ProductApiIT.generatesValidEan13AndLooksUpByBarcode` |
| TC-PROD-08 | FR-PROD-03 | Đ/L | `GET /skus/lookup?barcode=` mã có tồn 7; mã không tồn tại | 200 kèm `available = 7`; 404 `PR40401` | IT `ProductApiIT.generatesValidEan13...` |
| TC-PROD-09 | FR-PROD-04 | Đ | Đổi giá SKU sau khi đã có đơn Draft | Giá mới 250.000; đơn cũ giữ `unitPrice` 100.000 | IT `ProductApiIT.changesPricesWithoutAffectingExistingOrders` |
| TC-PROD-10 | FR-PROD-04 | L | Đổi giá bán lẻ về 0 | 400 `PR40001` | IT như trên |
| TC-PROD-11 | FR-PROD-05 | Đ | Tìm theo tên (không phân biệt hoa/thường), theo mã SKU gần đúng, theo mã vạch chính xác; mã vạch cắt ngắn không khớp | Đúng kết quả; không khớp → 0 | IT `ProductApiIT.searchesByNameSkuAndBarcodeWithPaging` |
| TC-PROD-12 | FR-PROD-05 | Đ | 26 sản phẩm: trang 0 và trang 1; lọc `status=INACTIVE/ACTIVE` sau khi ngừng bán 1 sản phẩm | Mặc định 20/trang, trang 1 có 6; lọc đúng | IT như trên |
| TC-PROD-13 | FR-PROD-01 | Đ/L | Tạo danh mục cha–con; tạo trùng tên cùng cấp; tạo trùng tên khác cấp | 201; 409 `PR40901`; khác cấp 201 | IT `ProductApiIT.categoryTreeNamesAndDeleteRules` |
| TC-PROD-14 | FR-PROD-01 | L | Đặt cha của danh mục gốc là con của nó (vòng lặp) | 400 `PR40001` | IT như trên |
| TC-PROD-15 | FR-PROD-01 | L | Xóa danh mục còn danh mục con / còn sản phẩm; xóa thương hiệu còn sản phẩm; tạo thương hiệu trùng tên | 409 `PR40902`; 409 `PR40901` | IT như trên |

## INV và COST

| Mã | FR | Loại | Mô tả và bước thực hiện | Kết quả mong đợi | Tự động |
|---|---|---|---|---|---|
| TC-INV-01 | FR-INV-02, FR-COST-01 | Đ | **Nghiệm thu Sprint 2:** SKU của sản phẩm 3 biến thể; xác nhận phiếu 10 × 100.000 rồi 30 × 120.000 | `avg_cost = 115.000`; tồn 40; sổ cái 2 dòng IN/PURCHASE, `balance_after` = 10 rồi 40 | IT `InventoryIT.sprint2Acceptance_...` |
| TC-INV-02 | FR-COST-01 | Đ/L | 11 ví dụ tính tay giá vốn (tồn cũ 0, giá bằng nhau, làm tròn HALF_UP…) và số lượng nhập ≤ 0 | Đúng từng ví dụ; số lượng ≤ 0 → lỗi | U `CostCalculatorTest` |
| TC-INV-03 | FR-INV-01 | L | `UPDATE`/`DELETE` trực tiếp trên `inventory_transaction` bằng SQL | PostgreSQL từ chối bằng trigger, dữ liệu không đổi | IT `InventoryIT.ledgerRejectsUpdateAndDeleteAtDatabaseLevel` |
| TC-INV-04 | FR-INV-02 | L | Sửa hoặc xác nhận lại phiếu đã xác nhận | 409 `IV40902`; tồn chỉ tăng một lần | IT `InventoryIT.confirmedReceiptCannotBeChangedOrConfirmedAgain` |
| TC-INV-05 | FR-INV-02 | Đ | Tạo phiếu Nháp, sửa thêm dòng, xem tồn/sổ cái, xác nhận | Nháp chưa ghi sổ cái, chưa có tồn; sửa được; xác nhận ghi 2 dòng | IT `InventoryIT.draftReceiptIsEditableAndWritesNothingUntilConfirmed` |
| TC-INV-06 | FR-INV-02 | L | Phiếu nhập số lượng 0, đơn giá âm, không dòng, chi nhánh lạ, SKU lạ, phiếu không tồn tại | 400 `IV40001` (SKU lạ: 404 `PR40401`; phiếu lạ: 404 `IV40401`) | IT `InventoryIT.validatesReceiptInputAndSuppliers` |
| TC-INV-07 | FR-INV-05 | Đ | Thêm nhà cung cấp, ngưng dùng (`active=false`), vẫn liệt kê được | 201; 200; danh sách có 1 | IT như trên |
| TC-INV-08 | FR-INV-01, NFR-PERF-02 (mức sổ cái) | L | 50 luồng cùng xuất 1 cái từ SKU còn đúng 1 cái | Đúng 1 thành công, 49 `IV40901`; tồn 0, không âm; sổ cái 2 dòng | IT `InventoryIT.concurrentOutNeverOversells` |
| TC-INV-09 | FR-COST-01 | Đ | 8 phiếu nhập cùng SKU xác nhận đồng thời, cùng giá 100.000 | Tồn 80; `avg_cost` 100.000 (khóa dòng SKU chống ghi đè) | IT `InventoryIT.concurrentReceiptsOnSameSkuKeepAverageCostConsistent` |

## ORD và cô lập tenant

| Mã | FR/NFR | Loại | Mô tả và bước thực hiện | Kết quả mong đợi | Tự động |
|---|---|---|---|---|---|
| TC-ORD-01 | FR-ORD-01 | Đ | `POST /orders` 2 dòng hàng | 201; `DRAFT`, kênh `MANUAL`, tổng = Σ giá lẻ × SL, lịch sử 1 dòng (null → DRAFT) | IT `OrderApiIT.createsDraftOrderWithTotalAndHistory` |
| TC-ORD-02 | FR-ORD-01 | L | Đơn không dòng, SL 0, chi nhánh lạ, SKU lạ, SKU ngừng bán, đơn không tồn tại | 400 `OD40001`; SKU lạ 404 `PR40401`; đơn lạ 404 `OD40401` | IT `OrderApiIT.validatesOrderInput` |
| TC-ORD-03 | FR-ORD-02 | Đ/L | 6 bước hợp lệ; bước đầu từ "mới" chỉ Draft/Reserved; 12 bước bị chặn; Completed/Cancelled là cuối | Hợp lệ đúng; bị chặn → `OD40901` | U `OrderStateMachineTest` (20 case) |
| TC-ORD-04 | FR-ORD-03 | Đ | Danh sách đơn lọc `status/channel/branchId/from/to`, phân trang | Đúng tổng và trang | IT `OrderApiIT.listsOrdersWithFiltersAndPaging` |
| TC-TEN-01 | NFR-TENANT-01 | L | Tenant B đọc/sửa/xóa/xác nhận dữ liệu tenant A (sản phẩm, SKU, phiếu nhập, đơn, danh mục, thương hiệu, chi nhánh, mã vạch) | Mọi trường hợp **404**; danh sách của B rỗng; dữ liệu A không đổi | IT `TenantIsolationIT.tenantBCannotReadOrChangeTenantAData` |
| TC-ARC-01 | NFR-MAINT-01 | — | Kiểm tra phụ thuộc lớp | `domain` không phụ thuộc lớp ngoài; `application` không phụ thuộc `web`/`infrastructure`; controller không gọi repository | U `ArchitectureTest` (6 luật) |

> Ghi chú: TC-TEN-01 và TC-ARC-01 tính thêm vào 44 case nhưng thuộc nhóm phi chức năng; ba case TC-AUTH-13…15 gộp nhiều bước kiểm tra theo cùng một luồng vì chia sẻ dữ liệu.

## Test chưa tự động hóa (làm thủ công cuối sprint)

| Mã | Mô tả | Trạng thái |
|---|---|---|
| TC-UI-01 | Admin: đăng ký cửa hàng → đăng nhập → thấy menu đúng vai trò | Một phần: đã chạy tay trên trình duyệt đăng nhập, danh sách sản phẩm, phiếu nhập, tồn kho (hiển thị dữ liệu mẫu thật); chưa kiểm tra menu theo từng vai trò (Owner/Staff/Cashier) và chưa thử đăng ký cửa hàng mới qua giao diện |
| TC-UI-02 | Admin: tạo sản phẩm 3 biến thể, sinh mã vạch, in tem | Chưa chạy tay (chỉ build) |
| TC-UI-03 | Admin: tạo phiếu nhập Nháp → sửa → xác nhận | Chưa chạy tay (API đã có test tự động) |
| TC-UI-04 | POS: quét mã + Enter, quét lại tăng SL, F2/F9/Esc, chặn vượt tồn | Logic giỏ hàng có 6 test Vitest; thao tác bàn phím trên trình duyệt chưa chạy tay |

## Kết quả lần chạy 04/11/2026

Tổng hợp: 62 unit test + 25 test tích hợp, 0 lỗi (lần chạy `./mvnw clean verify` ngày 04/11/2026; chi tiết ở `backend/target/surefire-reports` và `failsafe-reports`). Kiểm thử thủ công API xác thực: [Báo cáo test thủ công API Sprint 1](B%C3%A1o%20c%C3%A1o%20test%20th%E1%BB%A7%20c%C3%B4ng%20API%20Sprint%201.md). Truy vết yêu cầu – test: [RTM v2](RTM%20v2.md).
