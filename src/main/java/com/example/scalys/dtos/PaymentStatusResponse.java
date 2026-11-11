package com.example.scalys.dtos;

import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.OrderType;
import java.time.Instant;
import java.math.BigDecimal;

public record PaymentStatusResponse(String reference, PaymentStatus paymentStatus,
                                    OrderStatus orderStatus, BigDecimal totalPrice,
                                    OrderType orderType, Instant holdExpiresAt, String publicCode,
                                    boolean balancePayment) {
    public static PaymentStatusResponse from(Order order) {
        return new PaymentStatusResponse(order.getPaymentReference(), order.getPaymentStatus(),
                order.getStatus(), order.getPaymentAmount(), order.getOrderType(), order.getHoldExpiresAt(),
                order.getPublicCode(), false);
    }

    public static PaymentStatusResponse fromBalance(Order order) {
        return new PaymentStatusResponse(order.getBalanceReference(), order.getBalanceStatus(),
                order.getStatus(), order.getBalanceAmount(), order.getOrderType(), order.getHoldExpiresAt(),
                order.getPublicCode(), true);
    }
}
