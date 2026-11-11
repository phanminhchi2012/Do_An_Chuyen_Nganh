package com.example.scalys;

import com.example.scalys.controllers.UnifiedLoginController;
import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.dtos.UnifiedLoginRequest;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.StaffPrincipal;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCreateCustomerHttpTests {
    @Autowired MockMvc http;
    @Autowired AdminUserService users;
    @Autowired StaffAccountRepository staffAccounts;
    @Autowired CustomerAccountRepository customers;
    @Autowired UnifiedLoginController login;

    @Test
    void adminCanCreateCustomerWhoCanUseTheSharedLogin() throws Exception {
        String email = "admin-created-" + UUID.randomUUID() + "@example.test";
        var admin = User.withUsername("test-admin").password("unused").roles("ADMIN").build();
        var session = sessionFor(admin, admin.getAuthorities());
        try {
            String token = csrf(session);
            http.perform(post("/api/admin/users/customers").session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(customerBody(email)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.totalPetSpent").value(0));
            var customer = customers.findByEmail(email).orElseThrow();
            assertFalse(customer.getPasswordHash().contains("password-123"));
            assertEquals("CUSTOMER", login.login(new UnifiedLoginRequest(email, "password-123"),
                    new MockHttpServletRequest(), new MockHttpServletResponse()).role());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void staffMayCreateCustomerButCannotCreateStaff() throws Exception {
        String staffEmail = "staff-creator-" + UUID.randomUUID() + "@example.test";
        var created = users.createStaff(new AdminUserRequests.CreateStaff(staffEmail, "Staff", "password-123"));
        StaffPrincipal principal = StaffPrincipal.from(staffAccounts.findById(created.id()).orElseThrow());
        var session = sessionFor(principal, principal.getAuthorities());
        try {
            String token = csrf(session);
            String email = "staff-created-" + UUID.randomUUID() + "@example.test";
            http.perform(post("/api/admin/users/customers").session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(customerBody(email)))
                    .andExpect(status().isCreated());
            http.perform(post("/api/admin/users/staff").session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"new-staff@example.test\",\"fullName\":\"New Staff\",\"password\":\"password-123\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private MockHttpSession sessionFor(Object principal,
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private String csrf(MockHttpSession session) throws Exception {
        String response = http.perform(get("/api/admin/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        if (!matcher.find()) throw new AssertionError("Missing CSRF token");
        return matcher.group(1);
    }

    private String customerBody(String email) {
        return "{\"email\":\"" + email + "\",\"fullName\":\"Customer\","
                + "\"phone\":\"0912345678\",\"address\":\"Shop\",\"password\":\"password-123\"}";
    }
}
