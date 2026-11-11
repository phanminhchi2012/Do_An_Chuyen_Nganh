package com.example.scalys.dtos;

import java.util.List;

public record CustomerOrderPage(List<CustomerOrderResponse> orders, int page, int totalPages, long totalOrders) {}
