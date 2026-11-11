package com.example.scalys;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.CustomerGoogleLoginService;
import com.example.scalys.services.CustomerSessionService;
import com.example.scalys.services.OrderService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CustomerGoogleLoginIntegrationTests {
    @Autowired CustomerGoogleLoginService google;
    @Autowired CustomerAccountService passwords;
    @Autowired CustomerAccountRepository accounts;
    @Autowired CustomerSessionService sessions;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;

    @Test
    void googleSignupSignsInWithoutPhoneOrAddress() {
        String email = "google-" + UUID.randomUUID() + "@example.test";
        String subject = "google-sub-" + UUID.randomUUID();
        var request = new MockHttpServletRequest();

        google.begin(request, subject, email, "Google Customer");
        var account = sessions.required(request);
        assertEquals(email, account.getEmail());
        assertEquals(subject, account.getGoogleSubject());
        assertNull(account.getPhone());
        assertNull(account.getAddress());
        assertNull(account.getPasswordHash());
        assertThrows(ResponseStatusException.class, () -> passwords.login(
                new CustomerAccountRequests.Login(email, "any-password"), "127.0.0.1"));

        var nextRequest = new MockHttpServletRequest();
        google.begin(nextRequest, subject, email, "Google Customer");
        assertEquals(account.getId(), sessions.required(nextRequest).getId());
    }

    @Test
    void verifiedGoogleEmailLinksExistingCustomerWithoutContactForm() {
        String email = "google-link-" + UUID.randomUUID() + "@example.test";
        String subject = "google-sub-" + UUID.randomUUID();
        var existing = passwords.register(new CustomerAccountRequests.Register(
                "Existing Customer", email, "0912345678", "Existing address", "password-123"));
        var request = new MockHttpServletRequest();

        google.begin(request, subject, email, "Google Name");
        var linked = sessions.required(request);
        assertEquals(existing.getId(), linked.getId());
        assertEquals(subject, linked.getGoogleSubject());
        assertEquals("Existing address", linked.getAddress());
        assertEquals(subject, accounts.findById(existing.getId()).orElseThrow().getGoogleSubject());
    }

    @Test
    void differentGoogleIdentityCannotTakeLinkedAccount() {
        String email = "google-linked-" + UUID.randomUUID() + "@example.test";
        google.begin(new MockHttpServletRequest(), "first-subject-" + UUID.randomUUID(), email, "Customer");
        assertThrows(ResponseStatusException.class, () -> google.begin(
                new MockHttpServletRequest(), "second-subject-" + UUID.randomUUID(), email, "Customer"));
    }

    @Test
    void googleCustomerProvidesPhoneAndAddressOnlyWhenOrdering() {
        String email = "google-checkout-" + UUID.randomUUID() + "@example.test";
        var request = new MockHttpServletRequest();
        google.begin(request, "checkout-subject-" + UUID.randomUUID(), email, "Google Customer");
        var account = sessions.required(request);
        Product product = new Product("Google checkout test", null, BigDecimal.valueOf(100000),
                null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 2);
        product = products.saveAndFlush(product);

        var order = orderService.createOrder(new OrderRequest(product.getId(), account.getFullName(),
                "0912345678", "Delivery address", FulfillmentMethod.DELIVERY, null, 1,
                null, null, false, false, email), "127.0.0.1", account.getId());

        assertEquals("0912345678", orders.findById(order.id()).orElseThrow().getPhone());
        assertEquals("Delivery address", orders.findById(order.id()).orElseThrow().getAddress());
        assertNull(accounts.findById(account.getId()).orElseThrow().getPhone());
    }
}
