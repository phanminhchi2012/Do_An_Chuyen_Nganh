package com.example.scalys;

import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.dtos.OrderResponse;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.ProductService;
import com.example.scalys.services.VnpayPaymentService;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "VNPAY_TMN_CODE=TESTCODE",
        "VNPAY_HASH_SECRET=local-test-secret-only",
        "VNPAY_RETURN_URL=http://127.0.0.1:2003/orders/payment-result",
        "VNPAY_IPN_URL=https://shop.example.test/api/payments/vnpay/ipn"
})
@Transactional
class VnpayPaymentIntegrationTests {
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired ProductService productService;
    @Autowired VnpayPaymentService payments;
    @Autowired CustomerAccountService customerAccounts;
    @Autowired PromotionService promotions;

    @Test
    void failedDiscountedPaymentReleasesCodeAndSuccessfulRetryConsumesIt() {
        var account = customerAccounts.register(new CustomerAccountRequests.Register("Khách thử",
                "vnpay-promo-" + UUID.randomUUID() + "@example.test", "0912345678",
                "Địa chỉ thử", "password-123"));
        String code = promotions.myCodes(account.getId()).stream()
                .filter(item -> item.newCustomerOnly()).findFirst().orElseThrow().code();
        Product product = new Product("Payment promo supply", null, BigDecimal.valueOf(200_000),
                null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 3);
        products.saveAndFlush(product);
        OrderRequest request = new OrderRequest(product.getId(), "Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null, 1, null, PaymentMethod.VNPAY,
                false, false, account.getEmail(), List.of(code));
        OrderResponse first = orderService.createOrder(request, "127.0.0.1", account.getId());
        assertEquals("19000000", query(URI.create(first.checkoutUrl()).getRawQuery()).get("vnp_Amount"));
        assertEquals("00", payments.handleIpn(callback(first.paymentReference(), "19000000", "24", "02"))
                .get("RspCode"));
        assertEquals("AVAILABLE", promotions.myCodes(account.getId()).stream()
                .filter(item -> item.code().equals(code)).findFirst().orElseThrow().status());
        OrderResponse retry = orderService.createOrder(request, "127.0.0.1", account.getId());
        assertEquals("00", payments.handleIpn(callback(retry.paymentReference(), "19000000", "00", "00"))
                .get("RspCode"));
        assertEquals("USED", promotions.myCodes(account.getId()).stream()
                .filter(item -> item.code().equals(code)).findFirst().orElseThrow().status());
    }

    @Test
    void signedIpnPaysSnapshotAmountAndCannotBeReplayedToChangeState() {
        Product product = new Product("Payment test supply", null, BigDecimal.valueOf(100_000), null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 3);
        product = products.saveAndFlush(product);
        OrderResponse created = orderService.createOrder(request(product, 2), "127.0.0.1");
        assertEquals(PaymentStatus.PENDING, created.paymentStatus());
        assertEquals(1, product.getStockQuantity());

        Map<String, String> checkout = query(URI.create(created.checkoutUrl()).getRawQuery());
        assertEquals("20000000", checkout.get("vnp_Amount"));
        assertEquals(created.paymentReference(), checkout.get("vnp_TxnRef"));
        assertEquals("TESTCODE", checkout.get("vnp_TmnCode"));
        assertTrue(checkout.get("vnp_SecureHash").matches("[0-9a-f]{128}"));
        Map<String, String> unsignedCheckout = new HashMap<>(checkout);
        unsignedCheckout.remove("vnp_SecureHash");
        assertEquals(sign(unsignedCheckout), checkout.get("vnp_SecureHash"));

        Map<String, String> callback = callback(created.paymentReference(), "20000000", "00", "00");
        assertTrue(payments.verifyReturn(callback).gatewayReportedSuccess());
        assertEquals(PaymentStatus.PENDING, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        callback.put("vnp_SecureHash", "0".repeat(128));
        assertEquals("97", payments.handleIpn(callback).get("RspCode"));
        assertEquals(PaymentStatus.PENDING, orders.findById(created.id()).orElseThrow().getPaymentStatus());

        callback = callback(created.paymentReference(), "10000000", "00", "00");
        assertEquals("04", payments.handleIpn(callback).get("RspCode"));
        callback = callback(created.paymentReference(), "20000000", "00", "00");
        assertEquals("00", payments.handleIpn(callback).get("RspCode"));
        assertEquals("02", payments.handleIpn(callback).get("RspCode"));
        assertEquals(PaymentStatus.PAID, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        assertEquals(1, product.getStockQuantity());
        assertEquals(OrderStatus.CONFIRMED, orders.findById(created.id()).orElseThrow().getStatus());
        assertThrows(ResponseStatusException.class, () -> orderService.updateStatus(created.id(), OrderStatus.CANCELLED));
    }

    @Test
    void failedIpnReleasesPetAndLateSuccessRequiresReview() {
        Product pet = new Product("Payment test pet", "Gecko", BigDecimal.valueOf(300_000),
                null, null, ProductStatus.AVAILABLE);
        pet.updateCatalog(ProductKind.PET, null, 1);
        products.saveAndFlush(pet);
        OrderResponse created = orderService.createOrder(request(pet, 1));
        assertEquals(ProductStatus.HELD, pet.getStatus());
        assertThrows(ResponseStatusException.class,
                () -> productService.updateStatus(pet.getId(), ProductStatus.SOLD));

        assertEquals("00", payments.handleIpn(callback(created.paymentReference(), "30000000", "24", "02")).get("RspCode"));
        assertEquals(PaymentStatus.FAILED, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, orders.findById(created.id()).orElseThrow().getStatus());
        assertEquals(ProductStatus.AVAILABLE, pet.getStatus());

        assertEquals("00", payments.handleIpn(callback(created.paymentReference(), "30000000", "00", "00")).get("RspCode"));
        assertEquals(PaymentStatus.REVIEW, orders.findById(created.id()).orElseThrow().getPaymentStatus());
    }

    @Test
    void abandonedPaymentExpiresAndReleasesStock() {
        Product supply = new Product("Payment timeout supply", null, BigDecimal.valueOf(100_000), null, null, ProductStatus.AVAILABLE);
        supply.updateCatalog(ProductKind.SUPPLY, ProductCategory.FOOD, 2);
        supply = products.saveAndFlush(supply);
        OrderResponse created = orderService.createOrder(request(supply, 1));
        assertEquals(1, supply.getStockQuantity());
        ReflectionTestUtils.setField(orders.findById(created.id()).orElseThrow(), "paymentExpiresAt",
                Instant.now().minus(20, ChronoUnit.MINUTES));
        payments.expirePending(created.id());
        assertEquals(PaymentStatus.EXPIRED, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        assertEquals(2, supply.getStockQuantity());
    }

    @Test
    void suspiciousPaymentStaysReservedForManualReview() {
        Product pet = new Product("Review test pet", "Gecko", BigDecimal.valueOf(300_000),
                null, null, ProductStatus.AVAILABLE);
        pet.updateCatalog(ProductKind.PET, null, 1);
        products.saveAndFlush(pet);
        OrderResponse created = orderService.createOrder(request(pet, 1));
        assertEquals("00", payments.handleIpn(callback(created.paymentReference(), "30000000", "07", "04")).get("RspCode"));
        assertEquals(PaymentStatus.REVIEW, orders.findById(created.id()).orElseThrow().getPaymentStatus());
        assertEquals(OrderStatus.NEW, orders.findById(created.id()).orElseThrow().getStatus());
        assertEquals(ProductStatus.HELD, pet.getStatus());
    }

    private OrderRequest request(Product product, int quantity) {
        return new OrderRequest(product.getId(), "Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null, quantity, null, PaymentMethod.VNPAY,
                false, false, "khach@example.test");
    }

    private Map<String, String> callback(String reference, String amount, String response, String status) {
        Map<String, String> values = new HashMap<>();
        values.put("vnp_TmnCode", "TESTCODE");
        values.put("vnp_TxnRef", reference);
        values.put("vnp_Amount", amount);
        values.put("vnp_ResponseCode", response);
        values.put("vnp_TransactionStatus", status);
        values.put("vnp_TransactionNo", "123456789");
        values.put("vnp_SecureHash", sign(values));
        return values;
    }

    private String sign(Map<String, String> values) {
        try {
            String payload = new TreeMap<>(values).entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec("local-test-secret-only".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Map<String, String> query(String rawQuery) {
        return java.util.Arrays.stream(rawQuery.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> URLDecoder.decode(part[0], StandardCharsets.UTF_8),
                        part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
    }
}
