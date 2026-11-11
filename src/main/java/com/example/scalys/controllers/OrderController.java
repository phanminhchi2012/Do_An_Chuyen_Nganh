package com.example.scalys.controllers;

import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.OrderResponse;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.ZalopayCheckoutService;
import com.example.scalys.services.CustomerSessionService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ZalopayCheckoutService zalopayCheckout;
    private final CustomerSessionService sessions;

    public OrderController(OrderService orderService, ZalopayCheckoutService zalopayCheckout,
                           CustomerSessionService sessions) {
        this.orderService = orderService;
        this.zalopayCheckout = zalopayCheckout;
        this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request, HttpServletRequest httpRequest) {
        return orderService.createOrder(request, httpRequest.getRemoteAddr(), accountId(httpRequest));
    }

    @PostMapping("/cart")
    @ResponseStatus(HttpStatus.CREATED)
    public List<OrderResponse> createCartOrders(@Valid @RequestBody CartOrderRequest request,
                                                HttpServletRequest httpRequest) {
        return orderService.createCartOrders(request, accountId(httpRequest));
    }

    @PostMapping("/cart/vnpay")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createCartVnpay(@Valid @RequestBody CartOrderRequest request,
                                         HttpServletRequest httpRequest) {
        return orderService.createCartVnpay(request, httpRequest.getRemoteAddr(), accountId(httpRequest));
    }

    @PostMapping("/zalopay")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createZalopay(@Valid @RequestBody OrderRequest request,
                                       HttpServletRequest httpRequest) {
        return zalopayCheckout.createSingle(request, accountId(httpRequest));
    }

    @PostMapping("/cart/zalopay")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createCartZalopay(@Valid @RequestBody CartOrderRequest request,
                                           HttpServletRequest httpRequest) {
        return zalopayCheckout.createCart(request, accountId(httpRequest));
    }

    private Long accountId(HttpServletRequest request) {
        var account = sessions.optional(request);
        return account == null ? null : account.getId();
    }
}
