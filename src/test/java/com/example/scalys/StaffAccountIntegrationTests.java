package com.example.scalys;

import com.example.scalys.controllers.UnifiedLoginController;
import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.UnifiedLoginRequest;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.StaffPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class StaffAccountIntegrationTests {
    @Autowired AdminUserService users;
    @Autowired CustomerAccountService customers;
    @Autowired UnifiedLoginController login;

    @Test
    void staffUsesSharedLoginAndCannotBeRegisteredAsCustomer() {
        String email = "staff-" + UUID.randomUUID() + "@example.test";
        var staff = users.createStaff(new AdminUserRequests.CreateStaff(email, "Nhân viên thử", "password-123"));
        assertTrue(staff.enabled());
        assertThrows(ResponseStatusException.class, () -> customers.register(
                new CustomerAccountRequests.Register("Khách", email, "0912345678",
                        "Địa chỉ thử", "password-123")));
        var request = new MockHttpServletRequest();
        try {
            assertEquals("STAFF", login.login(new UnifiedLoginRequest(email, "password-123"),
                    request, new MockHttpServletResponse()).role());
            var context = (SecurityContext) request.getSession(false).getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertTrue(context.getAuthentication().getPrincipal() instanceof StaffPrincipal);
            assertTrue(context.getAuthentication().getAuthorities().stream().anyMatch(authority ->
                    "ROLE_STAFF".equals(authority.getAuthority())));
            users.updateStaff(staff.id(), new AdminUserRequests.UpdateStaff(email, "Nhân viên thử", false, null));
            assertThrows(ResponseStatusException.class, () -> users.loginStaff(email, "password-123", "127.0.0.1"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void disablingCustomerKeepsAccountForOrderHistoryButBlocksLogin() {
        String email = "managed-" + UUID.randomUUID() + "@example.test";
        var customer = customers.register(new CustomerAccountRequests.Register("Khách thử", email,
                "0912345678", "Địa chỉ thử", "password-123"));
        users.disableCustomer(customer.getId());
        assertFalse(users.list().customers().stream().filter(item -> item.id().equals(customer.getId()))
                .findFirst().orElseThrow().enabled());
        assertThrows(ResponseStatusException.class, () -> customers.login(
                new CustomerAccountRequests.Login(email, "password-123"), "127.0.0.1"));
        assertThrows(ResponseStatusException.class, () -> customers.get(customer.getId()));
    }
}
