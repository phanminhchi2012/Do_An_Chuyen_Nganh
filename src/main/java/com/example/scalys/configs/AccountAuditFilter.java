package com.example.scalys.configs;

import com.example.scalys.models.CustomerAccount;
import com.example.scalys.services.AccountAuditService;
import com.example.scalys.services.CustomerSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class AccountAuditFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AccountAuditFilter.class);
    private static final Pattern ID = Pattern.compile("/(\\d+)(?:/|$)");
    private static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([A-Z_]+)\"");
    private final AccountAuditService audit;
    private final CustomerSessionService customers;

    public AccountAuditFilter(AccountAuditService audit, CustomerSessionService customers) {
        this.audit = audit;
        this.customers = customers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();
        if (!(method.equals("POST") || method.equals("PUT") || method.equals("PATCH")
                || method.equals("DELETE")) || !request.getServletPath().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        Actor before = actor(request);
        String path = request.getServletPath();
        boolean statusChange = path.matches("/api/admin/(orders|products|contacts|warranty-claims)/[0-9]+/status");
        HttpServletRequest auditedRequest = statusChange ? new ContentCachingRequestWrapper(request, 1024) : request;
        chain.doFilter(auditedRequest, response);
        if (response.getStatus() < 200 || response.getStatus() >= 300) return;
        Actor actor = actor(request);
        if (actor == null) actor = before;
        if (actor == null) return;
        String action = action(method, path);
        if (statusChange) {
            String body = new String(((ContentCachingRequestWrapper) auditedRequest).getContentAsByteArray(),
                    StandardCharsets.UTF_8);
            Matcher status = STATUS.matcher(body);
            if (status.find()) action = path.contains("/orders/")
                    ? "SET_ORDER_" + status.group(1) : path.contains("/contacts/")
                    ? "SET_CONTACT_" + status.group(1) : path.contains("/warranty-claims/")
                    ? "SET_WARRANTY_" + status.group(1) : "SET_PRODUCT_" + status.group(1);
        }
        Matcher id = ID.matcher(path);
        String targetId = id.find() ? id.group(1) : null;
        String type = path.contains("/products") ? "PRODUCT" : path.contains("/orders") ? "ORDER"
                : path.contains("/promotions") ? "PROMOTION" : path.contains("/users") ? "USER"
                : path.contains("/contacts") ? "CONTACT" : path.contains("/warranty-claims") ? "WARRANTY"
                : path.contains("/account") ? "ACCOUNT" : "SESSION";
        try {
            audit.record(actor.role(), actor.id(), action, type, targetId,
                    path.substring(0, Math.min(path.length(), 250)), request.getRemoteAddr());
        } catch (RuntimeException failure) {
            LOG.error("Could not record account action audit", failure);
        }
    }

    private Actor actor(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getAuthorities().stream().anyMatch(item -> "ROLE_ADMIN".equals(item.getAuthority())))
                return new Actor("ADMIN", auth.getName());
            if (auth.getAuthorities().stream().anyMatch(item -> "ROLE_STAFF".equals(item.getAuthority())))
                return new Actor("STAFF", auth.getName());
        }
        try {
            CustomerAccount customer = customers.optional(request);
            return customer == null ? null : new Actor("CUSTOMER", customer.getId().toString());
        } catch (RuntimeException expired) {
            return null;
        }
    }

    private String action(String method, String path) {
        if (path.endsWith("/login")) return "LOGIN";
        if (path.endsWith("/logout")) return "LOGOUT";
        if (path.contains("/not-received")) return "MARK_NOT_RECEIVED";
        if (path.contains("/confirm-balance")) return "CONFIRM_BALANCE";
        if (path.contains("/products")) return method + "_PRODUCT";
        if (path.contains("/promotions")) return method + "_PROMOTION";
        if (path.contains("/contacts")) return method + "_CONTACT";
        if (path.contains("/warranty-claims")) return method + "_WARRANTY";
        if (path.contains("/orders")) return method + "_ORDER";
        if (path.contains("/users/staff")) return method + "_STAFF";
        if (path.contains("/users/customers")) return method + "_CUSTOMER";
        return method + "_ACCOUNT";
    }

    private record Actor(String role, String id) {}
}
