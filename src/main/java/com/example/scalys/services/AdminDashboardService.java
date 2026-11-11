package com.example.scalys.services;

import com.example.scalys.dtos.AdminDashboardResponse;
import com.example.scalys.repositories.AdminDashboardRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AdminDashboardRepository repository;

    public AdminDashboardService(AdminDashboardRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public AdminDashboardResponse overview() {
        LocalDate today = LocalDate.now(VIETNAM);
        LocalDate week = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate month = today.withDayOfMonth(1);
        Instant now = Instant.now();
        var revenue = new AdminDashboardResponse.Revenue(
                period(today, today.plusDays(1), today.minusDays(1), today),
                period(week, week.plusWeeks(1), week.minusWeeks(1), week),
                period(month, month.plusMonths(1), month.minusMonths(1), month));
        var orders = new AdminDashboardResponse.Orders(repository.orderCount("NEW"),
                repository.successfulOrderCount(), repository.cancelledOrderCount());
        var customers = new AdminDashboardResponse.Customers(repository.customerCount(null, null),
                repository.customerCount(start(month), start(month.plusMonths(1))),
                repository.returningBuyers());
        Map<String, BigDecimal> daily = repository.revenueTrend(start(today.minusDays(6)),
                start(today.plusDays(1))).stream().collect(java.util.stream.Collectors.toMap(
                        AdminDashboardResponse.RevenuePoint::day,
                        AdminDashboardResponse.RevenuePoint::amount));
        var trend = IntStream.range(0, 7).mapToObj(index -> {
            String day = today.minusDays(6L - index).toString();
            return new AdminDashboardResponse.RevenuePoint(day, daily.getOrDefault(day, BigDecimal.ZERO));
        }).toList();
        return new AdminDashboardResponse(revenue, orders, customers, trend,
                repository.categories(), repository.recentOrders(), repository.lowStock(), now);
    }

    private AdminDashboardResponse.Period period(LocalDate from, LocalDate until,
                                                 LocalDate previousFrom, LocalDate previousUntil) {
        BigDecimal current = repository.revenue(start(from), start(until));
        BigDecimal previous = repository.revenue(start(previousFrom), start(previousUntil));
        BigDecimal change = previous.signum() == 0 ? null : current.subtract(previous)
                .multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
        return new AdminDashboardResponse.Period(current, previous, change);
    }

    private Instant start(LocalDate date) { return date.atStartOfDay(VIETNAM).toInstant(); }
}
