package com.example.scalys;

import com.example.scalys.models.CustomerAccount;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.CustomerPetSpendingRepository;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.StaffPrincipal;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerPetSpendingIntegrationTests {
    @Autowired CustomerAccountRepository customers;
    @Autowired CustomerPetSpendingRepository spending;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired AdminUserService adminUsers;
    @Autowired StaffAccountRepository staffAccounts;
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void countsNetCompletedPetOrdersOnlyForTheLinkedCustomer() throws Exception {
        var customer = customers.saveAndFlush(new CustomerAccount(
                "pet-spend-" + UUID.randomUUID() + "@example.test", "unused", "Pet buyer",
                "0912345678", "Shop"));
        var other = customers.saveAndFlush(new CustomerAccount(
                "pet-spend-" + UUID.randomUUID() + "@example.test", "unused", "Other buyer",
                "0912345679", "Shop"));
        var pet = new Product("Spending test pet", "Pet", BigDecimal.valueOf(100000),
                null, null, ProductStatus.AVAILABLE);
        pet.updateCatalog(ProductKind.PET, null, 10);
        pet = products.saveAndFlush(pet);
        var supply = new Product("Spending test supply", null, BigDecimal.valueOf(100000),
                null, null, ProductStatus.AVAILABLE);
        supply.updateCatalog(ProductKind.SUPPLY, ProductCategory.FOOD, 10);
        supply = products.saveAndFlush(supply);

        var discounted = saveOrder(pet, customer, 2, OrderStatus.CONFIRMED);
        jdbc.update("UPDATE orders SET discount_amount = 25000 WHERE id = ?", discounted.getId());
        saveOrder(pet, customer, 1, OrderStatus.NEW);
        saveOrder(pet, customer, 1, OrderStatus.CANCELLED);
        saveOrder(pet, null, 1, OrderStatus.CONFIRMED);
        saveOrder(supply, customer, 1, OrderStatus.CONFIRMED);
        saveOrder(pet, other, 1, OrderStatus.CONFIRMED);

        var depositOnly = saveOrder(pet, customer, 1, OrderStatus.NEW);
        depositOnly.beginDeposit(Instant.now());
        depositOnly.beginVnpayPayment("DEP" + UUID.randomUUID().toString().substring(0, 12), Instant.now().plusSeconds(900));
        depositOnly.setPaymentStatus(PaymentStatus.PAID);
        orders.saveAndFlush(depositOnly);

        var completedHold = saveOrder(pet, customer, 1, OrderStatus.NEW);
        completedHold.beginDeposit(Instant.now());
        completedHold.beginVnpayPayment("DEP" + UUID.randomUUID().toString().substring(0, 12), Instant.now().plusSeconds(900));
        completedHold.setPaymentStatus(PaymentStatus.PAID);
        completedHold.markBalancePaid(Instant.now());
        completedHold.setStatus(OrderStatus.CONFIRMED);
        orders.saveAndFlush(completedHold);

        assertEquals(0, BigDecimal.valueOf(275000).compareTo(spending.totalFor(customer.getId())));
        assertEquals(0, BigDecimal.valueOf(275000).compareTo(spending.totals().get(customer.getId())));
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(spending.totalFor(other.getId())));
        var customerRow = adminUsers.list().customers().stream()
                .filter(row -> row.id().equals(customer.getId())).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(275000).compareTo(customerRow.totalPetSpent()));

        var staff = adminUsers.createStaff(new AdminUserRequests.CreateStaff(
                "pet-spend-staff-" + UUID.randomUUID() + "@example.test", "Staff", "password-123"));
        var principal = StaffPrincipal.from(staffAccounts.findById(staff.id()).orElseThrow());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            var response = http.perform(get("/api/admin/users").session(session))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var rows = json.readTree(response).path("customers");
            var returned = rows.valueStream().filter(row -> row.path("id").asLong() == customer.getId())
                    .findFirst().orElseThrow();
            assertEquals(0, BigDecimal.valueOf(275000).compareTo(returned.path("totalPetSpent").decimalValue()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Order saveOrder(Product product, CustomerAccount customer, int quantity, OrderStatus status) {
        var order = new Order(product, "Buyer", "0912345678", "Shop",
                FulfillmentMethod.PICKUP, null, quantity, null);
        order.setPublicCode(UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        order.setCustomerAccount(customer);
        order.setStatus(status);
        return orders.saveAndFlush(order);
    }
}
