package com.acme.salary.insights;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InsightsExceptionHandler {

    @ExceptionHandler(NoSalaryDataException.class)
    public ResponseEntity<Map<String, String>> handleNoData(NoSalaryDataException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}
