package com.example.scalys.dtos;

import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.OrderType;
import java.math.BigDecimal;

import java.time.Instant;

public record OrderResponse(Long id, Long productId, int quantity, OrderStatus status,
                            PaymentMethod paymentMethod, PaymentStatus paymentStatus,
                            String paymentReference, String checkoutUrl, String zalopayQrCode, Instant createdAt,
                            OrderType orderType, BigDecimal paymentAmount, Instant holdExpiresAt,
                            String publicCode, BigDecimal subtotal, BigDecimal discountAmount,
                            String promotionCode) {
    public static OrderResponse from(Order order, String checkoutUrl) {
        return new OrderResponse(order.getId(), order.getProduct().getId(), order.getQuantity(),
                order.getStatus(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getPaymentReference(), checkoutUrl, null, order.getCreatedAt(),
                order.getOrderType(), order.getPaymentAmount(), order.getHoldExpiresAt(),
                order.getPublicCode(), order.getSubtotal(), order.getDiscountAmount(),
                order.getPromotionCode() == null ? null : order.getPromotionCode().getCode());
    }

    public OrderResponse withCheckoutUrl(String url) {
        return new OrderResponse(id, productId, quantity, status, paymentMethod, paymentStatus,
                paymentReference, url, zalopayQrCode, createdAt, orderType, paymentAmount, holdExpiresAt,
                publicCode, subtotal, discountAmount, promotionCode);
    }

    public OrderResponse withZalopayCheckout(String url, String qrCode) {
        return new OrderResponse(id, productId, quantity, status, paymentMethod, paymentStatus,
                paymentReference, url, qrCode, createdAt, orderType, paymentAmount, holdExpiresAt,
                publicCode, subtotal, discountAmount, promotionCode);
    }
}
