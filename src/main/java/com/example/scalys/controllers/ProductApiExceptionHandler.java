package com.example.scalys.controllers;

import com.example.scalys.dtos.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {ProductController.class, AdminProductController.class,
        ProductImageController.class})
public class ProductApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new ApiError(error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getAllErrors().stream()
                .map(object -> object.getDefaultMessage())
                .findFirst().orElse("Dữ liệu sản phẩm không hợp lệ.");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ApiError("Dữ liệu sản phẩm không hợp lệ."));
    }
}
