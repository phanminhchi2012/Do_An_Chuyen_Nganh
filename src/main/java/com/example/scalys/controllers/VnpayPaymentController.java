package com.example.scalys.controllers;

import com.example.scalys.dtos.PaymentStatusResponse;
import com.example.scalys.dtos.VnpayReturnResponse;
import com.example.scalys.services.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
public class VnpayPaymentController {
    private final VnpayPaymentService payments;

    public VnpayPaymentController(VnpayPaymentService payments) { this.payments = payments; }

    @GetMapping("/availability")
    public Map<String, Boolean> availability() {
        return Map.of("available", payments.isAvailable());
    }

    @GetMapping("/orders/{reference}")
    public PaymentStatusResponse status(@PathVariable String reference) {
        checkReference(reference);
        return payments.getStatus(reference);
    }

    @GetMapping("/verify-return")
    public VnpayReturnResponse verifyReturn(HttpServletRequest request) {
        try {
            return payments.verifyReturn(singleVnpayParameters(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu VNPAY không hợp lệ.");
        }
    }

    @GetMapping("/ipn")
    public Map<String, String> ipn(HttpServletRequest request) {
        try {
            return payments.handleIpn(singleVnpayParameters(request));
        } catch (IllegalArgumentException exception) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }
    }

    private Map<String, String> singleVnpayParameters(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (var entry : request.getParameterMap().entrySet()) {
            if (!entry.getKey().startsWith("vnp_")) continue;
            if (entry.getValue().length != 1) {
                throw new IllegalArgumentException("Duplicate VNPAY parameter");
            }
            fields.put(entry.getKey(), entry.getValue()[0]);
        }
        return fields;
    }

    private void checkReference(String reference) {
        if (!reference.matches("[0-9a-f]{32}")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch.");
        }
    }
}
