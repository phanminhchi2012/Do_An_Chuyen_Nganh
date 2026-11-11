package com.example.scalys;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuditPaginationHttpTests {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void adminSeesPagedAuditAndGuestsCannotReadIt() throws Exception {
        http.perform(get("/api/admin/audit?page=0")).andExpect(status().isUnauthorized());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin-test", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM account_audit_events", Long.class);
        try {
            http.perform(get("/api/admin/audit?page=0").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEvents").value(total))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.events.length()").value((int) Math.min(total, 15)));
            http.perform(get("/api/admin/audit?page=-1").session(session))
                    .andExpect(status().isBadRequest());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
