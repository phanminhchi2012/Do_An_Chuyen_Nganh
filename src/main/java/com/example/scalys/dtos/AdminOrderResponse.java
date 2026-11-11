package com.example.scalys.dtos;

import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.BalanceMethod;
import java.math.BigDecimal;

import java.time.Instant;
import java.time.LocalDate;

public record AdminOrderResponse(
        Long id,
        Long productId,
        String productName,
        String customerName,
        String phone,
        FulfillmentMethod fulfillmentMethod,
        String address,
        String note,
        int quantity,
        LocalDate requestedDate,
        BigDecimal totalPrice,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus status,
        Instant createdAt,
        String publicCode,
        String email,
        OrderType orderType,
        Instant holdExpiresAt,
        BalanceMethod balanceMethod,
        PaymentStatus balanceStatus,
        BigDecimal paymentAmount,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String promotionCode
) {
    public static AdminOrderResponse from(Order order) {
        return new AdminOrderResponse(
                order.getId(), order.getProduct().getId(), order.getProduct().getName(),
                order.getCustomerName(), order.getPhone(), order.getFulfillmentMethod(),
                order.getAddress(), order.getNote(), order.getQuantity(), order.getRequestedDate(),
                order.getTotalPrice(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getStatus(), order.getCreatedAt(), order.getPublicCode(), order.getEmail(),
                order.getOrderType(), order.getHoldExpiresAt(), order.getBalanceMethod(),
                order.getBalanceStatus(), order.getPaymentAmount(), order.getSubtotal(),
                order.getDiscountAmount(), order.getPromotionCode() == null
                        ? null : order.getPromotionCode().getCode()
        );
    }
}
