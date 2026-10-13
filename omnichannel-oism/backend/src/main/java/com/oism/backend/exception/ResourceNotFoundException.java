package com.oism.backend.exception;

/**
 * Ngoại lệ khi không tìm thấy dữ liệu.
 */
// Ném ra khi không tìm thấy dữ liệu trong database
public class ResourceNotFoundException extends RuntimeException {

    // Khởi tạo ngoại lệ với thông điệp lỗi
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
