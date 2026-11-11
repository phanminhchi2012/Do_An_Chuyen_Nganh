package com.example.scalys.controllers;

import com.example.scalys.dtos.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {OrderController.class, OrderFollowupController.class,
        AdminOrderController.class, VnpayPaymentController.class})
public class OrderApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new ApiError(error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu đơn hàng không hợp lệ.");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ApiError("Dữ liệu đơn hàng không hợp lệ."));
    }
}
