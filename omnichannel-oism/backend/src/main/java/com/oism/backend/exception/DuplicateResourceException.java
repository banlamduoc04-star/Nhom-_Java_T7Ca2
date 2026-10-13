package com.oism.backend.exception;

/**
 * Ngoại lệ khi dữ liệu bị trùng lặp.
 */
// Ném ra khi dữ liệu bị trùng lặp (ví dụ: mã SKU đã tồn tại)
public class DuplicateResourceException extends RuntimeException {

    // Khởi tạo ngoại lệ với thông điệp lỗi
    public DuplicateResourceException(String message) {
        super(message);
    }
}
