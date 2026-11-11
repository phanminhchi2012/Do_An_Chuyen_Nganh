package com.example.scalys;

import com.example.scalys.services.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AdminDashboardIntegrationTests {
    @Autowired AdminDashboardService dashboard;

    @Test
    void overviewUsesStoredOrdersAndReturnsSevenRevenueDays() {
        var result = dashboard.overview();
        assertNotNull(result.revenue());
        assertNotNull(result.orders());
        assertNotNull(result.customers());
        assertEquals(7, result.revenueTrend().size());
    }
}
