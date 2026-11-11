package com.example.scalys.services;

import com.example.scalys.dtos.AuditEventPage;
import com.example.scalys.repositories.AccountAuditRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountAuditService {
    private final AccountAuditRepository audit;
    public AccountAuditService(AccountAuditRepository audit) { this.audit = audit; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String role, String actor, String action, String targetType,
                       String targetId, String path, String ip) {
        audit.record(role, actor, action, targetType, targetId, path, ip);
    }
    @Transactional(readOnly = true)
    public AuditEventPage page(int page) {
        if (page < 0 || page > 100_000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ.");
        int pageSize = 15;
        long total = audit.count();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new AuditEventPage(audit.page(page, pageSize), total, totalPages, page);
    }
}
