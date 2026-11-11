package com.example.scalys.repositories;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerPetSpendingRepository {
    private static final String COMPLETED_PET_PURCHASE = " FROM orders o JOIN products p ON p.id = o.product_id "
            + "WHERE o.customer_account_id IS NOT NULL AND p.kind = 'PET' AND o.status = 'CONFIRMED' "
            + "AND ((o.order_type = 'PURCHASE' "
            + "AND (o.payment_status = 'PAID' OR o.payment_method = 'PAY_LATER')) "
            + "OR (o.order_type = 'HOLD_DEPOSIT' AND o.payment_status = 'PAID' "
            + "AND o.balance_status = 'PAID'))";
    private static final String NET_TOTAL = "SUM(o.unit_price * o.quantity - o.discount_amount)";
    private final JdbcTemplate jdbc;

    public CustomerPetSpendingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, BigDecimal> totals() {
        return jdbc.query("SELECT o.customer_account_id AS account_id, " + NET_TOTAL + " AS total "
                        + COMPLETED_PET_PURCHASE + " GROUP BY o.customer_account_id",
                (rs, row) -> Map.entry(rs.getLong("account_id"), rs.getBigDecimal("total")))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public BigDecimal totalFor(Long accountId) {
        BigDecimal total = jdbc.queryForObject("SELECT COALESCE(" + NET_TOTAL + ", 0) "
                + COMPLETED_PET_PURCHASE + " AND o.customer_account_id = ?", BigDecimal.class, accountId);
        return total == null ? BigDecimal.ZERO : total;
    }
}
