package com.example.scalys.controllers;

import com.example.scalys.dtos.AdminDashboardResponse;
import com.example.scalys.services.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService dashboard;

    public AdminDashboardController(AdminDashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping
    public AdminDashboardResponse overview() { return dashboard.overview(); }
}
