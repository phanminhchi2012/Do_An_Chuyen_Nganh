package com.example.scalys;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.EmailOutboxRepository;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.CustomerOrderHistoryService;
import com.example.scalys.services.OrderService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CustomerOrderHistoryIntegrationTests {
    @Autowired CustomerAccountService accounts;
    @Autowired CustomerOrderHistoryService history;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired EmailOutboxRepository outbox;
    @Autowired JdbcTemplate jdbc;

    @Test
    void accountOnlySeesOrdersItPlacedAndGuestReceivesInvoiceCodeByEmail() {
        String email = "history-" + UUID.randomUUID() + "@example.test";
        var account = accounts.register(new CustomerAccountRequests.Register(
                "Khách có tài khoản", email, "0912345678", "Địa chỉ lưu", "password-123"));
        Product product = new Product("Đèn thử lịch sử", null, BigDecimal.valueOf(100000),
                null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 3);
        product = products.saveAndFlush(product);

        var accountOrder = orderService.createOrder(new OrderRequest(product.getId(), "Tên giả", "0987654321",
                null, FulfillmentMethod.PICKUP, null, 1, null, null, false, false,
                "someone-else@example.test"), "127.0.0.1", account.getId());
        var guestOrder = orderService.createOrder(new OrderRequest(product.getId(), "Khách vãng lai", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, null, false, false,
                email), "127.0.0.1");

        var linked = orders.findById(accountOrder.id()).orElseThrow();
        assertEquals(account.getId(), linked.getCustomerAccount().getId());
        assertEquals(account.getFullName(), linked.getCustomerName());
        assertEquals(account.getPhone(), linked.getPhone());
        assertEquals(email, linked.getEmail());
        assertEquals(null, orders.findById(guestOrder.id()).orElseThrow().getCustomerAccount());
        assertEquals(1, history.list(account.getId(), 0).totalOrders());
        assertEquals(accountOrder.publicCode(), history.list(account.getId(), 0).orders().getFirst().invoiceCode());
        assertFalse(history.list(account.getId(), 0).orders().stream()
                .anyMatch(line -> line.invoiceCode().equals(guestOrder.publicCode())));
        assertTrue(outbox.existsByOrderIdAndKind(guestOrder.id(), "ORDER_RECEIVED"));
        String emailBody = jdbc.queryForObject("SELECT body FROM email_outbox WHERE order_id = ? AND kind = 'ORDER_RECEIVED'",
                String.class, guestOrder.id());
        assertTrue(emailBody.contains(guestOrder.publicCode()));
        assertTrue(emailBody.contains("/order-lookup"));
    }
}
