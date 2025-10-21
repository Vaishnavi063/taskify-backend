package com.taskify.backend.utils;

public class ApiException extends RuntimeException {
    private final int statusCode;

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getstatusCode() {
        return statusCode;
    }
}

