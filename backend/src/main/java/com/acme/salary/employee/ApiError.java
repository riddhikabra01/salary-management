package com.acme.salary.employee;

import java.util.Map;

public record ApiError(String message, Map<String, String> errors) {

    static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
