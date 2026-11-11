package com.example.scalys.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminDashboardResponse(
        Revenue revenue,
        Orders orders,
        Customers customers,
        List<RevenuePoint> revenueTrend,
        List<CategorySale> categories,
        List<RecentOrder> recentOrders,
        List<LowStockProduct> lowStock,
        Instant generatedAt) {
    public record Revenue(Period day, Period week, Period month) {}
    public record Period(BigDecimal amount, BigDecimal previousAmount, BigDecimal changePercent) {}
    public record Orders(long newOrders, long successful, long cancelled) {}
    public record Customers(long total, long newThisMonth, long returningBuyers) {}
    public record RevenuePoint(String day, BigDecimal amount) {}
    public record CategorySale(String kind, String category, BigDecimal revenue) {}
    public record RecentOrder(Long id, String code, String customerName, BigDecimal total,
                              String status, Instant createdAt) {}
    public record LowStockProduct(Long id, String name, String category, int stockQuantity) {}
}
