# Báo cáo công việc

## 1. Tóm tắt
<!-- Làm gì, vì sao, phạm vi (module/FR nào trong kế hoạch) -->

- Sprint:
- Module: <!-- AUTH / PROD / INV / COST / ORD / RSE / POS / REP / SIM -->
- Mã yêu cầu: <!-- FR-... / NFR-... -->
- Người thực hiện:

## 2. Jira
| Task | Trạng thái | Ghi chú |
|---|---|---|
| JC-? | | |

## 3. Thay đổi chính
<!-- Liệt kê file/package quan trọng, endpoint mới hoặc đổi, migration Flyway (nếu có) -->

-

## 4. Kết quả kiểm chứng
| Kiểm tra | Kết quả |
|---|---|
| `./mvnw clean verify` | |
| Test đơn vị / tích hợp | |
| Chạy Docker (`docker compose up -d --build`) | |
| Thử API / Swagger | |

## 5. Lỗi gặp phải và cách xử lý
| Lỗi | Nguyên nhân | Xử lý |
|---|---|---|
| | | |

## 6. Ảnh hưởng và lưu ý khi merge
- Có đổi API / DB schema không:
- Có cần cập nhật biến môi trường không:
- Thành viên nào cần biết:

## 7. Việc còn lại
- [ ]

## Checklist
- [ ] Build xanh (`clean verify`)
- [ ] Đã test chạy bằng Docker
- [ ] Không đưa `docs/`, `target/`, file `.env` vào commit
- [ ] Đã cập nhật README nếu đổi cách chạy
