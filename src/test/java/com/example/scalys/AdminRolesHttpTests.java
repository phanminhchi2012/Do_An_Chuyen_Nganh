package com.example.scalys;

import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.repositories.StaffAccountRepository;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.StaffPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminRolesHttpTests {
    @Autowired MockMvc http;
    @Autowired AdminUserService users;
    @Autowired StaffAccountRepository staffAccounts;

    @Test
    void staffCanSeeDashboardAndUsersButCannotSeeAuditOrPromotions() throws Exception {
        String email = "staff-http-" + UUID.randomUUID() + "@example.test";
        var created = users.createStaff(new AdminUserRequests.CreateStaff(email, "Nhân viên", "password-123"));
        StaffPrincipal principal = StaffPrincipal.from(staffAccounts.findById(created.id()).orElseThrow());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            http.perform(get("/api/admin/dashboard").session(session)).andExpect(status().isOk());
            http.perform(get("/api/admin/users").session(session)).andExpect(status().isOk());
            http.perform(get("/api/admin/audit").session(session)).andExpect(status().isForbidden());
            http.perform(get("/api/admin/promotions").session(session)).andExpect(status().isForbidden());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
