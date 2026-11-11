package com.example.scalys.controllers;

import com.example.scalys.dtos.CloseHoldRequest;
import com.example.scalys.dtos.OrderLookupRequest;
import com.example.scalys.dtos.OrderLookupResponse;
import com.example.scalys.services.OrderFollowupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderFollowupController {
    private final OrderFollowupService service;

    public OrderFollowupController(OrderFollowupService service) { this.service = service; }

    @PostMapping("/lookup")
    public OrderLookupResponse lookup(@RequestBody OrderLookupRequest request, HttpServletRequest http) {
        return service.lookup(request, http.getRemoteAddr());
    }

    @PostMapping("/close-hold")
    public OrderLookupResponse close(@RequestBody CloseHoldRequest request, HttpServletRequest http) {
        return service.closeHold(request, http.getRemoteAddr());
    }

    @PostMapping("/cancel-hold")
    public OrderLookupResponse cancel(@RequestBody OrderLookupRequest request, HttpServletRequest http) {
        return service.cancelHold(request, http.getRemoteAddr());
    }
}
