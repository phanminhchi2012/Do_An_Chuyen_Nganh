package com.example.scalys.controllers;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/session")
    public Map<String, String> session(Authentication authentication) {
        String role = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())) ? "ADMIN" : "STAFF";
        return Map.of("username", authentication.getName(), "role", role);
    }
}
