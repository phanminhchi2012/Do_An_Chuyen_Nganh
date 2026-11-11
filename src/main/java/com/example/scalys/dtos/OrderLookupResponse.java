package com.example.scalys.dtos;

import com.example.scalys.models.BalanceMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderLookupResponse(String code, String productName, int quantity,
                                  String maskedPhone, String maskedEmail,
                                  OrderType orderType, OrderStatus status,
                                  BigDecimal totalPrice, BigDecimal paidAmount,
                                  BigDecimal balanceAmount, Instant holdExpiresAt,
                                  BalanceMethod balanceMethod, PaymentStatus balanceStatus,
                                  String checkoutUrl, BigDecimal subtotal,
                                  BigDecimal discountAmount, String promotionCode) {
    public static OrderLookupResponse from(Order order, String checkoutUrl) {
        String phone = order.getPhone();
        String email = order.getEmail();
        String maskedEmail = null;
        if (email != null && email.contains("@")) {
            int at = email.indexOf('@');
            maskedEmail = email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
        }
        return new OrderLookupResponse(order.getPublicCode(), order.getProduct().getName(),
                order.getQuantity(), "***" + phone.substring(Math.max(0, phone.length() - 3)),
                maskedEmail, order.getOrderType(), order.getStatus(), order.getTotalPrice(),
                order.getPaymentStatus() == PaymentStatus.PAID ? order.getPaymentAmount() : BigDecimal.ZERO,
                order.getOrderType() == OrderType.HOLD_DEPOSIT ? order.getBalanceAmount() : BigDecimal.ZERO,
                order.getHoldExpiresAt(), order.getBalanceMethod(), order.getBalanceStatus(), checkoutUrl,
                order.getSubtotal(), order.getDiscountAmount(), order.getPromotionCode() == null
                        ? null : order.getPromotionCode().getCode());
    }
}
