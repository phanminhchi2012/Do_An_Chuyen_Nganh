package com.example.scalys.dtos;

import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CustomerOrderResponse(String invoiceCode, Long productId, String productName,
                                    int quantity, BigDecimal totalPrice, OrderType orderType,
                                    OrderStatus status, PaymentStatus paymentStatus,
                                    Instant createdAt, Instant holdExpiresAt,
                                    BigDecimal subtotal, BigDecimal discountAmount, String promotionCode) {
    public static CustomerOrderResponse from(Order order) {
        return new CustomerOrderResponse(order.getPublicCode(), order.getProduct().getId(),
                order.getProduct().getName(), order.getQuantity(), order.getTotalPrice(),
                order.getOrderType(), order.getStatus(), order.getPaymentStatus(),
                order.getCreatedAt(), order.getHoldExpiresAt(), order.getSubtotal(),
                order.getDiscountAmount(), order.getPromotionCode() == null
                        ? null : order.getPromotionCode().getCode());
    }
}
