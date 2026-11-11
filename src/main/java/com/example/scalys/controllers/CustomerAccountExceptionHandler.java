package com.example.scalys.controllers;

import com.example.scalys.dtos.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = CustomerAccountController.class)
public class CustomerAccountExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new ApiError(error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getDefaultMessage()).findFirst().orElse("Thông tin tài khoản không hợp lệ.");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ApiError("Thông tin tài khoản không hợp lệ."));
    }
}
