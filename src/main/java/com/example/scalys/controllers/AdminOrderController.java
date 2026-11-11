package com.example.scalys.controllers;

import com.example.scalys.dtos.AdminOrderResponse;
import com.example.scalys.dtos.OrderStatusUpdateRequest;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.OrderFollowupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderFollowupService followups;

    public AdminOrderController(OrderService orderService, OrderFollowupService followups) {
        this.orderService = orderService;
        this.followups = followups;
    }

    @GetMapping
    public List<AdminOrderResponse> getOrders(@RequestParam(name = "status", required = false) OrderStatus status) {
        return orderService.getOrders(status);
    }

    @GetMapping("/pending-cod-count")
    public Map<String, Long> pendingCodCount() {
        return Map.of("count", orderService.countPendingCodOrders());
    }

    @PatchMapping("/{id}/status")
    public AdminOrderResponse updateStatus(@PathVariable("id") Long id,
                                           @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/confirm-balance")
    public AdminOrderResponse confirmBalance(@PathVariable("id") Long id) {
        return followups.confirmBalance(id);
    }

    @PatchMapping("/{id}/not-received")
    public AdminOrderResponse notReceived(@PathVariable("id") Long id) {
        return followups.notReceived(id);
    }
}
