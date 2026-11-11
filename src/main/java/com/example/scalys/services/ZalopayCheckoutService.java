package com.example.scalys.services;

import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.dtos.OrderResponse;
import org.springframework.stereotype.Service;

@Service
public class ZalopayCheckoutService {
    private final OrderService orders;
    private final ZalopayGateway gateway;

    public ZalopayCheckoutService(OrderService orders, ZalopayGateway gateway) {
        this.orders = orders;
        this.gateway = gateway;
    }

    public OrderResponse createSingle(OrderRequest request, Long accountId) {
        return start(orders.createSingleZalopayPending(request, accountId));
    }

    public OrderResponse createCart(CartOrderRequest request, Long accountId) {
        return start(orders.createCartZalopayPending(request, accountId));
    }

    private OrderResponse start(OrderResponse pending) {
        try {
            var result = gateway.createCheckout(pending.paymentReference(), pending.paymentAmount());
            return pending.withZalopayCheckout(result.checkoutUrl(), result.qrCode());
        } catch (RuntimeException exception) {
            orders.cancelUnstartedZalopay(pending.paymentReference());
            throw exception;
        }
    }
}
