package com.example.scalys;

import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.PromotionQuoteRequest;
import com.example.scalys.dtos.PromotionRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.OrderFollowupService;
import com.example.scalys.services.PromotionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PromotionIntegrationTests {
    @Autowired CustomerAccountService accounts;
    @Autowired CustomerAccountRepository accountRepository;
    @Autowired ProductRepository products;
    @Autowired PromotionService promotions;
    @Autowired OrderService orders;
    @Autowired OrderFollowupService followup;

    @Test
    void choosesHighestDiscountAndRejectsOtherAccountsCode() {
        var owner = customer();
        var stranger = customer();
        var product = supply(1_200_000);
        var issued = promotions.myCodes(owner.getId());
        String welcome = issued.stream().filter(code -> code.newCustomerOnly()).findFirst().orElseThrow().code();
        String bill = issued.stream().filter(code -> code.code().startsWith("BILL7-"))
                .findFirst().orElseThrow().code();
        var quote = promotions.quote(new PromotionQuoteRequest(
                List.of(new PromotionQuoteRequest.Item(product.getId(), 1)), List.of(welcome, bill)), owner.getId());
        assertEquals(bill, quote.appliedCode());
        assertEquals(0, quote.discountAmount().compareTo(BigDecimal.valueOf(84_000)));
        assertEquals(0, quote.total().compareTo(BigDecimal.valueOf(1_116_000)));
        assertThrows(ResponseStatusException.class, () -> promotions.quote(new PromotionQuoteRequest(
                List.of(new PromotionQuoteRequest.Item(product.getId(), 1)), List.of(bill)), stranger.getId()));
    }

    @Test
    void billSevenRequiresStrictlyMoreThanOneMillion() {
        var owner = customer();
        var product = supply(1_000_000);
        String bill = promotions.myCodes(owner.getId()).stream()
                .filter(code -> code.code().startsWith("BILL7-"))
                .findFirst().orElseThrow().code();
        var request = new PromotionQuoteRequest(
                List.of(new PromotionQuoteRequest.Item(product.getId(), 1)), List.of(bill));
        assertThrows(ResponseStatusException.class, () -> promotions.quote(request, owner.getId()));
    }

    @Test
    void cartDiscountIsDistributedWithoutExceedingAnyLinePrice() {
        var owner = customer();
        String prefix = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        promotions.create(new PromotionRequest(prefix, "Giảm thử 50%", BigDecimal.valueOf(50),
                BigDecimal.ZERO, false, true, Instant.now(), null));
        String code = promotions.myCodes(owner.getId()).stream()
                .filter(item -> item.code().startsWith(prefix + "-"))
                .findFirst().orElseThrow().code();
        var first = supply(1);
        var second = supply(1);
        var third = supply(1);
        var lines = orders.createCartOrders(new CartOrderRequest("Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null, List.of(
                new CartOrderRequest.Item(first.getId(), 1),
                new CartOrderRequest.Item(second.getId(), 1),
                new CartOrderRequest.Item(third.getId(), 1)), owner.getEmail(), List.of(code)), owner.getId());
        assertEquals(0, lines.stream().map(line -> line.discountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.valueOf(2)));
        lines.forEach(line -> assertTrue(line.discountAmount().compareTo(line.subtotal()) <= 0));
    }

    @Test
    void cancelledPurchaseReleasesCodeAndSuccessfulPurchaseConsumesItOnce() {
        var owner = customer();
        var product = supply(200_000);
        String welcome = promotions.myCodes(owner.getId()).stream()
                .filter(code -> code.newCustomerOnly()).findFirst().orElseThrow().code();
        var first = orders.createCartOrders(cart(product.getId(), owner.getEmail(), welcome), owner.getId()).getFirst();
        assertEquals(0, first.paymentAmount().compareTo(BigDecimal.valueOf(190_000)));
        assertEquals(0, first.discountAmount().compareTo(BigDecimal.valueOf(10_000)));
        assertEquals("RESERVED", status(owner.getId(), welcome));
        orders.updateStatus(first.id(), OrderStatus.CANCELLED);
        assertEquals("AVAILABLE", status(owner.getId(), welcome));
        assertNull(accountRepository.findById(owner.getId()).orElseThrow().getFirstPurchaseCompletedAt());

        var second = orders.createCartOrders(cart(product.getId(), owner.getEmail(), welcome), owner.getId()).getFirst();
        orders.updateStatus(second.id(), OrderStatus.CONFIRMED);
        var used = promotions.myCodes(owner.getId()).stream()
                .filter(item -> item.code().equals(welcome)).findFirst().orElseThrow();
        assertEquals("USED", used.status());
        assertNotNull(used.usedAt());
        assertEquals(List.of(second.publicCode()), used.usedOrderCodes());
        assertNotNull(accountRepository.findById(owner.getId()).orElseThrow().getFirstPurchaseCompletedAt());
        assertThrows(ResponseStatusException.class, () -> promotions.quote(new PromotionQuoteRequest(
                List.of(new PromotionQuoteRequest.Item(product.getId(), 1)), List.of(welcome)), owner.getId()));
        assertThrows(ResponseStatusException.class, () -> orders.createCartOrders(
                cart(product.getId(), owner.getEmail(), welcome), owner.getId()));
    }

    @Test
    void customerNotReceivingCodOrderRestoresWelcomeOffer() {
        var owner = customer();
        var product = supply(200_000);
        String welcome = promotions.myCodes(owner.getId()).stream()
                .filter(code -> code.newCustomerOnly()).findFirst().orElseThrow().code();
        var order = orders.createCartOrders(cart(product.getId(), owner.getEmail(), welcome), owner.getId()).getFirst();
        orders.updateStatus(order.id(), OrderStatus.CONFIRMED);
        assertEquals("USED", status(owner.getId(), welcome));
        followup.notReceived(order.id());
        var restored = promotions.myCodes(owner.getId()).stream()
                .filter(item -> item.code().equals(welcome)).findFirst().orElseThrow();
        assertEquals("AVAILABLE", restored.status());
        assertNull(restored.usedAt());
        assertTrue(restored.usedOrderCodes().isEmpty());
        assertNull(accountRepository.findById(owner.getId()).orElseThrow().getFirstPurchaseCompletedAt());
    }

    @Test
    void adminRejectsPastDatesAndDiscountOverFiftyPercent() {
        Instant today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        assertThrows(ResponseStatusException.class, () -> promotions.create(new PromotionRequest(
                "TESTMAX", "Quá mức", BigDecimal.valueOf(51), BigDecimal.ZERO,
                false, true, today, null)));
        assertThrows(ResponseStatusException.class, () -> promotions.create(new PromotionRequest(
                "TESTPAST", "Ngày cũ", BigDecimal.valueOf(10), BigDecimal.ZERO,
                false, true, today.minusSeconds(86_400), null)));
    }

    private String status(Long accountId, String code) {
        return promotions.myCodes(accountId).stream().filter(item -> item.code().equals(code))
                .findFirst().orElseThrow().status();
    }

    private com.example.scalys.models.CustomerAccount customer() {
        return accounts.register(new CustomerAccountRequests.Register("Khách thử",
                "promo-" + UUID.randomUUID() + "@example.test", "0912345678",
                "Địa chỉ thử", "password-123"));
    }

    private Product supply(long price) {
        Product product = new Product("Promo test " + UUID.randomUUID(), null,
                BigDecimal.valueOf(price), null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 5);
        return products.saveAndFlush(product);
    }

    private CartOrderRequest cart(Long productId, String email, String code) {
        return new CartOrderRequest("Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null,
                List.of(new CartOrderRequest.Item(productId, 1)), email, List.of(code));
    }
}
