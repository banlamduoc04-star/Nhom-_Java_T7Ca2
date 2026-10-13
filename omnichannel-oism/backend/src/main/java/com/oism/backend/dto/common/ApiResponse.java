package com.oism.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lớp cấu trúc phản hồi chuẩn cho toàn bộ API hệ thống.
 *
 * @param <T> Kiểu dữ liệu của kết quả trả về
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    // Trạng thái thành công hay thất bại của API
    private boolean success;

    // Thông báo phản hồi cho client
    private String message;

    // Dữ liệu trả về
    private T data;

    // Tạo nhanh phản hồi thành công kèm dữ liệu
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Thành công")
                .data(data)
                .build();
    }

    // Tạo nhanh phản hồi thành công kèm thông điệp và dữ liệu
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Tạo nhanh phản hồi thất bại kèm thông báo lỗi
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
