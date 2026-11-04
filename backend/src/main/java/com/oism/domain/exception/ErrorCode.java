package com.oism.domain.exception;

/**
 * Bang ma loi thong nhat (Ke hoach du an OISM, muc 4 - "Bang ma loi").
 * Ten hang so chinh la errorCode tra ve: {@code <TIEN TO><HTTP><SO>}, vi du IV40901.
 * Tien to: PR (san pham), AU (xac thuc), IV (ton kho), OD (don hang), RP (bao cao), SY (he thong).
 * Module nao can them ma moi thi bo sung vao day va cap nhat bang trong ke hoach.
 */
public enum ErrorCode {

    // AUTH
    AU40001(400, "Dữ liệu đăng ký/đăng nhập không hợp lệ"),
    AU40101(401, "Sai thông tin đăng nhập, thiếu token, token sai hoặc hết hạn"),
    AU40301(403, "Không đủ quyền thực hiện thao tác này"),
    AU40302(403, "Tài khoản đã bị khóa"),
    AU40401(404, "Không tìm thấy người dùng hoặc chi nhánh"),
    AU40901(409, "Email hoặc tên chi nhánh đã tồn tại"),
    AU40902(409, "Không ẩn được chi nhánh còn tồn kho hoặc còn đơn chưa kết thúc"),

    // PROD
    PR40001(400, "Dữ liệu sản phẩm/SKU/giá không hợp lệ"),
    PR40401(404, "Không tìm thấy sản phẩm, SKU, danh mục hoặc thương hiệu"),
    PR40901(409, "Trùng mã SKU hoặc mã vạch"),
    PR40902(409, "Không xóa được vì còn dữ liệu liên quan"),

    // INV
    IV40001(400, "Số lượng, đơn giá hoặc chi nhánh không hợp lệ"),
    IV40401(404, "Không tìm thấy phiếu nhập, phiếu chuyển hoặc phiên kiểm kê"),
    IV40901(409, "Không đủ hàng khả dụng"),
    IV40902(409, "Chứng từ đã xác nhận/duyệt, không sửa được"),

    // ORD
    OD40001(400, "Dữ liệu đơn hàng hoặc payload webhook không hợp lệ"),
    OD40101(401, "API key webhook sai"),
    OD40401(404, "Không tìm thấy đơn hàng"),
    OD40901(409, "Chuyển trạng thái đơn không hợp lệ"),
    OD40902(409, "Đơn trùng (cùng kênh và mã đơn ngoài)"),

    // REP
    RP40001(400, "Tham số báo cáo không hợp lệ"),

    // He thong
    SY50001(500, "Lỗi hệ thống");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
