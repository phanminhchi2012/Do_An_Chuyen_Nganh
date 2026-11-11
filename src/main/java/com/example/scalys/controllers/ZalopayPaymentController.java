package com.example.scalys.controllers;

import com.example.scalys.dtos.PaymentStatusResponse;
import com.example.scalys.dtos.ZalopayReturnResponse;
import com.example.scalys.services.ZalopayGateway;
import com.example.scalys.services.ZalopayPaymentService;
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
@RequestMapping("/api/payments/zalopay")
public class ZalopayPaymentController {
    private final ZalopayGateway gateway;
    private final ZalopayPaymentService payments;

    public ZalopayPaymentController(ZalopayGateway gateway, ZalopayPaymentService payments) {
        this.gateway = gateway;
        this.payments = payments;
    }

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
    public ZalopayReturnResponse verifyReturn(HttpServletRequest request) {
        Map<String, String> fields = singleParameters(request);
        if (!gateway.validRedirect(fields))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chữ ký ZaloPay không hợp lệ.");
        long amount;
        try {
            amount = Long.parseLong(fields.get("amount"));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền ZaloPay không hợp lệ.");
        }
        String reference = fields.get("apptransid");
        payments.verifyReturn(reference, amount);
        return new ZalopayReturnResponse(reference, "1".equals(fields.get("status")));
    }

    private Map<String, String> singleParameters(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (var entry : request.getParameterMap().entrySet()) {
            if (entry.getValue().length != 1)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu ZaloPay bị lặp.");
            fields.put(entry.getKey(), entry.getValue()[0]);
        }
        return fields;
    }

    private void checkReference(String reference) {
        if (!reference.matches("[0-9]{6}_[0-9a-f]{24}"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch.");
    }
}
