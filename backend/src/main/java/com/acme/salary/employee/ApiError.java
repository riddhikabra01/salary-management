package com.acme.salary.employee;

import java.util.Map;

/**
 * The one error shape every failing API call returns: a readable message, plus per-field
 * messages when a form fails validation (e.g. {"email": "Email must be valid"}).
 */
public record ApiError(String message, Map<String, String> errors) {

    // Shortcut for errors that have no field-level detail.
    static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
