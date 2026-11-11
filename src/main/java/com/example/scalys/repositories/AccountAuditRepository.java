package com.example.scalys.repositories;

import com.example.scalys.dtos.AuditEventResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountAuditRepository {
    private final JdbcTemplate jdbc;
    public AccountAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(String role, String actor, String action, String targetType,
                       String targetId, String path, String ip) {
        jdbc.update("INSERT INTO account_audit_events "
                + "(actor_role, actor_id, action, target_type, target_id, request_path, ip_address) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", role, actor, action, targetType, targetId, path, ip);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_audit_events", Long.class);
    }

    public List<AuditEventResponse> page(int page, int pageSize) {
        return jdbc.query("SELECT id, actor_role, actor_id, action, target_type, target_id, "
                + "request_path, ip_address, occurred_at FROM account_audit_events "
                + "ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new AuditEventResponse(rs.getLong("id"), rs.getString("actor_role"),
                        rs.getString("actor_id"), rs.getString("action"), rs.getString("target_type"),
                        rs.getString("target_id"), rs.getString("request_path"),
                        rs.getString("ip_address"), rs.getTimestamp("occurred_at").toInstant()),
                pageSize, page * pageSize);
    }
}
