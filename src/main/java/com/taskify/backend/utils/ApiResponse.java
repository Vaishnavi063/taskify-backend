package com.taskify.backend.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int status;

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return new ApiResponse<>(true, message, data, status);
    }

    public static <T> ApiResponse<T> failure(String message, int status) {
        return new ApiResponse<>(false, message, null, status);
    }

    public static <T> ApiResponse<T> failure(T data, String message, int status) {
        return new ApiResponse<>(false, message, data, status);
    }
}
