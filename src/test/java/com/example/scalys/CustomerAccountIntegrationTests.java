package com.example.scalys;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.controllers.CustomerAccountController;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.services.CustomerAccountService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CustomerAccountIntegrationTests {
    @Autowired CustomerAccountService accounts;
    @Autowired CustomerAccountRepository repository;
    @Autowired CustomerAccountController controller;

    @Test
    void registrationRequiresSeparateLoginBeforeProfileIsAvailable() {
        String email = "customer-" + UUID.randomUUID() + "@example.test";
        var request = new MockHttpServletRequest();
        controller.register(new CustomerAccountRequests.Register(
                "Khách thử", email, "0912345678", "Địa chỉ thử", "password-123"));

        assertThrows(ResponseStatusException.class, () -> controller.me(request));
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.createEmptyContext());
        controller.login(new CustomerAccountRequests.Login(email, "password-123"), request);
        assertEquals(email, controller.me(request).email());
        assertNull(request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void registrationHashesPasswordAndProfileAndPasswordChangesStayInDatabase() {
        String email = "customer-" + UUID.randomUUID() + "@example.test";
        assertThrows(ResponseStatusException.class, () -> accounts.register(new CustomerAccountRequests.Register(
                "Khách thử", email, "0912345678", " ", "password-123")));
        var account = accounts.register(new CustomerAccountRequests.Register(
                "Khách thử", email.toUpperCase(), "0912345678", "Địa chỉ ban đầu", "password-123"));

        assertEquals(email, account.getEmail());
        assertNotEquals("password-123", repository.findById(account.getId()).orElseThrow().getPasswordHash());
        assertEquals(account.getId(), accounts.login(new CustomerAccountRequests.Login(email, "password-123"),
                "127.0.0.1").getId());

        assertThrows(ResponseStatusException.class, () -> accounts.updateAddress(account.getId(),
                new CustomerAccountRequests.Address(null)));
        accounts.updateAddress(account.getId(), new CustomerAccountRequests.Address("Địa chỉ mới"));
        accounts.changePassword(account.getId(), new CustomerAccountRequests.Password("password-123", "new-password-456"));
        var saved = repository.findById(account.getId()).orElseThrow();
        assertEquals("Khách thử", saved.getFullName());
        assertEquals("0912345678", saved.getPhone());
        assertEquals("Địa chỉ mới", saved.getAddress());
        assertEquals(1, saved.getSessionVersion());
        assertFalse(saved.getPasswordHash().contains("password-123"));
        assertThrows(ResponseStatusException.class, () -> accounts.login(
                new CustomerAccountRequests.Login(email, "password-123"), "127.0.0.1"));
        assertEquals(account.getId(), accounts.login(new CustomerAccountRequests.Login(email, "new-password-456"),
                "127.0.0.1").getId());
    }
}
