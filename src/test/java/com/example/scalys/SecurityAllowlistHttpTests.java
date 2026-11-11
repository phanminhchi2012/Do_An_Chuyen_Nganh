package com.example.scalys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAllowlistHttpTests {
    @Autowired MockMvc http;

    @Test
    void existingPublicAndCustomerRoutesRemainReachableButNewApiIsClosed() throws Exception {
        http.perform(get("/api/products")).andExpect(status().isOk());
        http.perform(get("/api/account/csrf")).andExpect(status().isOk());
        http.perform(get("/api/payments/vnpay/availability")).andExpect(status().isOk());
        http.perform(get("/api/payments/zalopay/availability")).andExpect(status().isOk());
        http.perform(get("/api/account/me")).andExpect(status().isUnauthorized());
        http.perform(get("/api/account/promotions")).andExpect(status().isUnauthorized());
        http.perform(get("/api/not-yet-classified")).andExpect(status().isUnauthorized());
        http.perform(post("/api/auth/login")).andExpect(status().isForbidden());
    }

    @Test
    void evenAdminCannotReachAnUnclassifiedApi() throws Exception {
        var admin = User.withUsername("allowlist-admin").password("unused").roles("ADMIN").build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                admin, null, admin.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            http.perform(get("/api/admin/dashboard").session(session)).andExpect(status().isOk());
            http.perform(get("/api/admin/audit").session(session)).andExpect(status().isOk());
            http.perform(get("/api/not-yet-classified").session(session)).andExpect(status().isForbidden());
            http.perform(get("/api/admin/not-yet-classified").session(session)).andExpect(status().isForbidden());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
