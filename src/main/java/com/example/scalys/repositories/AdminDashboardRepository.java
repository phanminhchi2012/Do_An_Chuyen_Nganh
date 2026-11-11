package com.example.scalys.repositories;

import com.example.scalys.dtos.AdminDashboardResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminDashboardRepository {
    private static final String SUCCESS = "order_type = 'PURCHASE' AND status = 'CONFIRMED' "
            + "AND (payment_status = 'PAID' OR payment_method = 'PAY_LATER')";
    private static final String SUCCESS_WITH_ALIAS = "o.order_type = 'PURCHASE' AND o.status = 'CONFIRMED' "
            + "AND (o.payment_status = 'PAID' OR o.payment_method = 'PAY_LATER')";
    private static final String NET = "(unit_price * quantity - discount_amount)";
    private final JdbcTemplate jdbc;

    public AdminDashboardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public BigDecimal revenue(Instant from, Instant until) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(" + NET + "), 0) FROM orders WHERE "
                + SUCCESS + " AND created_at >= ? AND created_at < ?", BigDecimal.class,
                Timestamp.from(from), Timestamp.from(until));
    }

    public long orderCount(String status) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    public long successfulOrderCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE " + SUCCESS, Long.class);
        return count == null ? 0 : count;
    }

    public long cancelledOrderCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE status IN "
                + "('CANCELLED', 'HOLD_EXPIRED', 'NOT_RECEIVED')", Long.class);
        return count == null ? 0 : count;
    }

    public long customerCount(Instant from, Instant until) {
        String sql = "SELECT COUNT(*) FROM customer_accounts";
        Long count = from == null ? jdbc.queryForObject(sql, Long.class)
                : jdbc.queryForObject(sql + " WHERE created_at >= ? AND created_at < ?", Long.class,
                        Timestamp.from(from), Timestamp.from(until));
        return count == null ? 0 : count;
    }

    public long returningBuyers() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT customer_account_id FROM orders "
                + "WHERE customer_account_id IS NOT NULL AND " + SUCCESS + " GROUP BY customer_account_id "
                + "HAVING COUNT(DISTINCT COALESCE(checkout_group, public_code)) >= 2) buyers", Long.class);
        return count == null ? 0 : count;
    }

    public List<AdminDashboardResponse.RevenuePoint> revenueTrend(Instant from, Instant until) {
        return jdbc.query("SELECT (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day, "
                + "COALESCE(SUM(" + NET + "), 0) AS revenue FROM orders WHERE " + SUCCESS
                + " AND created_at >= ? AND created_at < ? GROUP BY day ORDER BY day",
                (rs, row) -> new AdminDashboardResponse.RevenuePoint(
                        rs.getObject("day", LocalDate.class).toString(), rs.getBigDecimal("revenue")),
                Timestamp.from(from), Timestamp.from(until));
    }

    public List<AdminDashboardResponse.CategorySale> categories() {
        return jdbc.query("SELECT p.kind, COALESCE(p.category, 'PET') AS category, "
                + "SUM(" + NET + ") AS revenue FROM orders o JOIN products p ON p.id = o.product_id "
                + "WHERE " + SUCCESS_WITH_ALIAS
                + " GROUP BY p.kind, p.category ORDER BY revenue DESC LIMIT 6",
                (rs, row) -> new AdminDashboardResponse.CategorySale(rs.getString("kind"),
                        rs.getString("category"), rs.getBigDecimal("revenue")));
    }

    public List<AdminDashboardResponse.RecentOrder> recentOrders() {
        return jdbc.query("SELECT id, public_code, customer_name, " + NET + " AS total, status, created_at "
                + "FROM orders ORDER BY created_at DESC, id DESC LIMIT 5",
                (rs, row) -> new AdminDashboardResponse.RecentOrder(rs.getLong("id"),
                        rs.getString("public_code"), rs.getString("customer_name"),
                        rs.getBigDecimal("total"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    public List<AdminDashboardResponse.LowStockProduct> lowStock() {
        return jdbc.query("SELECT id, name, category, stock_quantity FROM products "
                + "WHERE kind = 'SUPPLY' AND status = 'AVAILABLE' "
                + "AND stock_quantity BETWEEN 1 AND 5 ORDER BY stock_quantity, id LIMIT 5",
                (rs, row) -> new AdminDashboardResponse.LowStockProduct(rs.getLong("id"),
                        rs.getString("name"), rs.getString("category"), rs.getInt("stock_quantity")));
    }
}
