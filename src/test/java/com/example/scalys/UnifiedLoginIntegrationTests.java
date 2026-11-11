package com.example.scalys;

import com.example.scalys.controllers.UnifiedLoginController;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.UnifiedLoginRequest;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.CustomerSessionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UnifiedLoginIntegrationTests {
    private static final String ADMIN_USERNAME = "admin-test@example.test";
    private static final String ADMIN_PASSWORD = "admin-test-password";
    private static final String ADMIN_HASH = new BCryptPasswordEncoder().encode(ADMIN_PASSWORD);

    @DynamicPropertySource
    static void adminCredentials(DynamicPropertyRegistry properties) {
        properties.add("ADMIN_USERNAME", () -> ADMIN_USERNAME);
        properties.add("ADMIN_PASSWORD_HASH", () -> ADMIN_HASH);
    }

    @Autowired UnifiedLoginController login;
    @Autowired CustomerAccountService accounts;
    @Autowired CustomerSessionService sessions;

    @Test
    void configuredAdminEmailRoutesToAdminAndCustomerEmailRoutesToCustomer() {
        assertThrows(ResponseStatusException.class, () -> accounts.register(
                new CustomerAccountRequests.Register("Admin", ADMIN_USERNAME, "0912345678",
                        "Test address", "customer-password")));
        String customerEmail = "login-" + UUID.randomUUID() + "@example.test";
        accounts.register(new CustomerAccountRequests.Register(
                "Khách thử", customerEmail, "0912345678", "Địa chỉ thử", "customer-password"));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        try {
            assertEquals("ADMIN", login.login(new UnifiedLoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD),
                    request, response).role());
            var adminContext = (SecurityContext) request.getSession(false).getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertEquals(ADMIN_USERNAME, adminContext.getAuthentication().getName());

            assertEquals("CUSTOMER", login.login(new UnifiedLoginRequest(customerEmail, "customer-password"),
                    request, new MockHttpServletResponse()).role());
            assertEquals(customerEmail, sessions.required(request).getEmail());
            assertNull(request.getSession(false).getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
