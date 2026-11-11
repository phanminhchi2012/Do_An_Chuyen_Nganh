package com.example.scalys.controllers;

import com.example.scalys.dtos.AuditEventPage;
import com.example.scalys.services.AccountAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {
    private final AccountAuditService audit;
    public AdminAuditController(AccountAuditService audit) { this.audit = audit; }
    @GetMapping public AuditEventPage page(@RequestParam(defaultValue = "0") int page) {
        return audit.page(page);
    }
}
