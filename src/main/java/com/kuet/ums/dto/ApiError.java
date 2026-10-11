package com.kuet.ums.dto;

import java.util.Map;

public record ApiError(int status, String code, String message, Map<String, String> fields) {
    public ApiError(int status, String code, String message) {
        this(status, code, message, Map.of());
    }
}
