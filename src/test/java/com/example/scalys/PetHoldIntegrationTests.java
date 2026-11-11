package com.example.scalys;

import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.CloseHoldRequest;
import com.example.scalys.dtos.OrderLookupRequest;
import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.BalanceMethod;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderFollowupService;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.VnpayPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "VNPAY_TMN_CODE=TESTCODE",
        "VNPAY_HASH_SECRET=local-test-secret-only",
        "VNPAY_RETURN_URL=http://127.0.0.1:2003/orders/payment-result",
        "VNPAY_LOCAL_RETURN_CONFIRM=true"
})
@Transactional
class PetHoldIntegrationTests {
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired OrderFollowupService followups;
    @Autowired VnpayPaymentService payments;
    @Autowired JdbcTemplate jdbc;

    @Test
    void petAllowsMultipleUnitsButRejectsOrdersBeyondRemainingStock() {
        Product pet = pet("Pet quantity test", 20, 100_000);
        var first = orderService.createOrder(new OrderRequest(pet.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 10, null, PaymentMethod.PAY_LATER,
                false, false, "khach@example.test"));
        assertEquals(10, pet.getStockQuantity());
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> orderService.createOrder(
                new OrderRequest(pet.getId(), "Khách thử", "0912345678", null,
                        FulfillmentMethod.PICKUP, null, 11, null, PaymentMethod.PAY_LATER,
                        false, false, "khach@example.test")));
        assertTrue(error.getReason().contains("10"));
        orderService.updateStatus(first.id(), OrderStatus.CANCELLED);
        assertEquals(20, pet.getStockQuantity());
    }

    @Test
    void depositChargesOnlySelectedPetQuantityAndCanBeCancelled() {
        assertTrue(payments.isAvailable());
        Product pet = pet("Deposit quantity test", 12, 100_000);
        var created = orderService.createOrder(new OrderRequest(pet.getId(), "Khách thử", "0912345678",
                null, null, null, 10, null, PaymentMethod.VNPAY,
                true, true, "khach@example.test"));
        assertEquals(2, pet.getStockQuantity());
        assertEquals(FulfillmentMethod.UNDECIDED,
                orders.findById(created.id()).orElseThrow().getFulfillmentMethod());
        assertEquals(new BigDecimal("100000"), created.paymentAmount());
        String amount = parseQuery(URI.create(created.checkoutUrl()).getRawQuery()).get("vnp_Amount");
        assertEquals("10000000", amount);

        Map<String, String> callback = callback(created.paymentReference(), amount, "00", "00");
        assertTrue(payments.verifyReturn(callback).gatewayReportedSuccess());
        assertEquals(PaymentStatus.PAID, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        assertEquals("LOCAL_RETURN", jdbc.queryForObject(
                "SELECT source FROM vnpay_ipn_events WHERE reference = ? ORDER BY id DESC LIMIT 1",
                String.class, created.paymentReference()));
        assertEquals("02", payments.handleIpn(callback).get("RspCode"));
        var paid = orders.findById(created.id()).orElseThrow();
        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());
        assertEquals(OrderStatus.NEW, paid.getStatus());
        assertTrue(paid.getHoldExpiresAt().isAfter(Instant.now().plus(6, ChronoUnit.DAYS)));
        followups.cancelHold(new OrderLookupRequest(created.publicCode(), "0912345678"), "127.0.0.1");
        assertEquals(OrderStatus.CANCELLED, paid.getStatus());
        assertEquals(12, pet.getStockQuantity());
    }

    @Test
    void oneCartPaymentConfirmsEveryLineAtFullCartAmount() {
        Product pet = pet("Cart VNPAY pet", 2, 100_000);
        Product supply = new Product("Cart VNPAY supply", null, BigDecimal.valueOf(50_000),
                null, null, ProductStatus.AVAILABLE);
        supply.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 3);
        products.saveAndFlush(supply);
        var checkout = orderService.createCartVnpay(new CartOrderRequest("Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null,
                List.of(new CartOrderRequest.Item(pet.getId(), 2),
                        new CartOrderRequest.Item(supply.getId(), 2)), "khach@example.test"), "127.0.0.1");
        assertEquals("30000000", parseQuery(URI.create(checkout.checkoutUrl()).getRawQuery()).get("vnp_Amount"));
        assertEquals(0, pet.getStockQuantity());
        assertEquals(1, supply.getStockQuantity());
        assertEquals("00", payments.handleIpn(callback(checkout.paymentReference(), "30000000", "00", "00"))
                .get("RspCode"));
        assertEquals(2, orders.findByCheckoutGroupForUpdate(checkout.paymentReference()).size());
        assertTrue(orders.findByCheckoutGroupForUpdate(checkout.paymentReference()).stream()
                .allMatch(order -> order.getStatus() == OrderStatus.CONFIRMED
                        && order.getPaymentStatus() == PaymentStatus.PAID));
        assertEquals(ProductStatus.SOLD, pet.getStatus());
    }

    @Test
    void offlineBalanceNeedsAdminConfirmationAndNotReceivedReturnsPet() {
        Product pet = pet("Offline balance pet", 1, 500_000);
        var created = orderService.createOrder(new OrderRequest(pet.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, PaymentMethod.VNPAY,
                true, true, "khach@example.test"));
        assertEquals("00", payments.handleIpn(callback(created.paymentReference(), "5000000", "00", "00"))
                .get("RspCode"));
        followups.closeHold(new CloseHoldRequest(created.publicCode(), "0912345678", BalanceMethod.COD),
                "127.0.0.1");
        assertEquals(OrderStatus.NEW, orders.findById(created.id()).orElseThrow().getStatus());
        followups.confirmBalance(created.id());
        assertEquals(OrderStatus.CONFIRMED, orders.findById(created.id()).orElseThrow().getStatus());
        assertEquals(ProductStatus.SOLD, pet.getStatus());
        followups.notReceived(created.id());
        assertEquals(1, pet.getStockQuantity());
        assertEquals(ProductStatus.AVAILABLE, pet.getStatus());
    }

    @Test
    void sevenDayExpiryReleasesPetOnceAndKeepsDepositPaid() {
        Product pet = pet("Expiry deposit pet", 1, 500_000);
        var created = orderService.createOrder(new OrderRequest(pet.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, PaymentMethod.VNPAY,
                true, true, "khach@example.test"));
        payments.handleIpn(callback(created.paymentReference(), "5000000", "00", "00"));
        var order = orders.findById(created.id()).orElseThrow();
        ReflectionTestUtils.setField(order, "holdExpiresAt", Instant.now().minus(1, ChronoUnit.MINUTES));
        payments.expireDeposit(created.id());
        payments.expireDeposit(created.id());
        assertEquals(OrderStatus.HOLD_EXPIRED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(1, pet.getStockQuantity());
    }

    @Test
    void onlineBalanceConfirmsFromItsOwnSignedLocalReturn() {
        Product pet = pet("Online balance pet", 1, 500_000);
        var created = orderService.createOrder(new OrderRequest(pet.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, PaymentMethod.VNPAY,
                true, true, "khach@example.test"));
        payments.handleIpn(callback(created.paymentReference(), "5000000", "00", "00"));
        var closing = followups.closeHold(new CloseHoldRequest(created.publicCode(), "0912345678",
                BalanceMethod.VNPAY), "127.0.0.1");
        String reference = parseQuery(URI.create(closing.checkoutUrl()).getRawQuery()).get("vnp_TxnRef");
        assertEquals("45000000", parseQuery(URI.create(closing.checkoutUrl()).getRawQuery()).get("vnp_Amount"));
        assertEquals(OrderStatus.NEW, orders.findById(created.id()).orElseThrow().getStatus());
        assertEquals(reference, payments.verifyReturn(callback(reference, "45000000", "00", "00")).reference());
        assertTrue(payments.getStatus(reference).balancePayment());
        assertEquals("02", payments.handleIpn(callback(reference, "45000000", "00", "00")).get("RspCode"));
        assertEquals(OrderStatus.CONFIRMED, orders.findById(created.id()).orElseThrow().getStatus());
        assertEquals(PaymentStatus.PAID, orders.findById(created.id()).orElseThrow().getBalanceStatus());
        assertEquals(ProductStatus.SOLD, pet.getStatus());
    }

    private Product pet(String name, int stock, long price) {
        Product product = new Product(name, "Rắn", BigDecimal.valueOf(price), null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.PET, null, stock);
        return products.saveAndFlush(product);
    }

    private Map<String, String> callback(String reference, String amount, String response, String status) {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TmnCode", "TESTCODE");
        fields.put("vnp_TxnRef", reference);
        fields.put("vnp_Amount", amount);
        fields.put("vnp_ResponseCode", response);
        fields.put("vnp_TransactionStatus", status);
        fields.put("vnp_TransactionNo", "123456789");
        fields.put("vnp_SecureHash", sign(fields));
        return fields;
    }

    private String sign(Map<String, String> fields) {
        try {
            String query = new TreeMap<>(fields).entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec("local-test-secret-only".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(query.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Map<String, String> parseQuery(String query) {
        return java.util.Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> URLDecoder.decode(part[0], StandardCharsets.UTF_8),
                        part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
    }
}
