package com.example.scalys;

import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.OrderRequest;
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
import com.example.scalys.services.ZalopayCheckoutService;
import com.example.scalys.services.ZalopayGateway;
import com.example.scalys.services.ZalopayPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ZalopayPaymentIntegrationTests {
    private static final String REFERENCE = "261023_1234567890abcdef12345678";

    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired ZalopayCheckoutService checkout;
    @Autowired ZalopayPaymentService payments;
    @Autowired MockMvc http;
    @MockitoBean ZalopayGateway gateway;

    @BeforeEach
    void gatewayStubs() {
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.newReference()).thenReturn(REFERENCE);
        when(gateway.amountInDong(any(BigDecimal.class)))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).longValueExact());
        when(gateway.createCheckout(anyString(), any(BigDecimal.class)))
                .thenReturn(new ZalopayGateway.CheckoutData("https://sandbox.zalopay.vn/test-order", null));
    }

    @Test
    void publicHttpRouteCreatesCheckoutButOrdinaryOrderRouteRejectsZalopay() throws Exception {
        Product product = supply("ZaloPay HTTP product", 2, 100_000);
        String body = "{\"productId\":" + product.getId() + ",\"customerName\":\"Khách thử\"," +
                "\"phone\":\"0912345678\",\"email\":\"buyer@example.test\"," +
                "\"fulfillmentMethod\":\"PICKUP\",\"quantity\":1,\"paymentMethod\":\"ZALOPAY\"}";

        http.perform(get("/api/payments/zalopay/availability"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        http.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        http.perform(post("/api/orders/zalopay").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value(REFERENCE))
                .andExpect(jsonPath("$.checkoutUrl").value("https://sandbox.zalopay.vn/test-order"));
        assertEquals(1, product.getStockQuantity());
    }

    @Test
    void singlePurchaseIsConfirmedOnlyAfterGatewayQueryAndCannotBeAppliedTwice() {
        Product product = supply("ZaloPay test supply", 3, 100_000);
        var pending = checkout.createSingle(new OrderRequest(product.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 2, null, PaymentMethod.ZALOPAY,
                false, false, "buyer@example.test"), null);

        assertEquals(REFERENCE, pending.paymentReference());
        assertEquals(PaymentStatus.PENDING, pending.paymentStatus());
        assertNotNull(pending.checkoutUrl());
        assertEquals(1, product.getStockQuantity());

        when(gateway.query(REFERENCE)).thenReturn(new ZalopayGateway.QueryResult(
                1, 200_000, "1234567", Instant.now().toEpochMilli()));
        payments.refresh(REFERENCE);
        payments.refresh(REFERENCE);

        var paid = orders.findById(pending.id()).orElseThrow();
        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, paid.getStatus());
        assertEquals("1234567", paid.getZalopayTransactionId());
        assertEquals(1, product.getStockQuantity());
    }

    @Test
    void sharedSandboxQrIsReturnedWithoutTreatingItAsAWebUrl() throws Exception {
        when(gateway.createCheckout(anyString(), any(BigDecimal.class)))
                .thenReturn(new ZalopayGateway.CheckoutData(null, "000201" + "1234567890".repeat(16)));
        Product product = supply("ZaloPay QR test supply", 1, 10_000);
        String body = "{\"productId\":" + product.getId() + ",\"customerName\":\"Demo\"," +
                "\"phone\":\"0912345678\",\"email\":\"buyer@example.test\"," +
                "\"fulfillmentMethod\":\"PICKUP\",\"quantity\":1,\"paymentMethod\":\"ZALOPAY\"}";
        http.perform(post("/api/orders/zalopay").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zalopayQrCode").isString())
                .andExpect(jsonPath("$.checkoutUrl").isEmpty());
    }

    @Test
    void failedCartPaymentRestoresAllItems() {
        Product first = supply("ZaloPay first", 3, 100_000);
        Product second = supply("ZaloPay second", 4, 50_000);
        var pending = checkout.createCart(new CartOrderRequest("Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null,
                List.of(new CartOrderRequest.Item(first.getId(), 2),
                        new CartOrderRequest.Item(second.getId(), 3)), "buyer@example.test"), null);
        assertEquals(1, first.getStockQuantity());
        assertEquals(1, second.getStockQuantity());

        when(gateway.query(REFERENCE)).thenReturn(new ZalopayGateway.QueryResult(2, -1, "", 0));
        payments.refresh(REFERENCE);

        assertEquals(3, first.getStockQuantity());
        assertEquals(4, second.getStockQuantity());
        for (var line : orders.findByCheckoutGroupForUpdate(orders.findById(pending.id()).orElseThrow().getCheckoutGroup())) {
            assertEquals(PaymentStatus.FAILED, line.getPaymentStatus());
            assertEquals(OrderStatus.CANCELLED, line.getStatus());
        }
    }

    @Test
    void mismatchedAmountRequiresReviewAndReleasesStock() {
        Product product = supply("ZaloPay amount check", 2, 100_000);
        var pending = checkout.createSingle(new OrderRequest(product.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, PaymentMethod.ZALOPAY,
                false, false, "buyer@example.test"), null);
        when(gateway.query(REFERENCE)).thenReturn(new ZalopayGateway.QueryResult(
                1, 99_000, "1234567", Instant.now().toEpochMilli()));
        payments.refresh(REFERENCE);

        var reviewed = orders.findById(pending.id()).orElseThrow();
        assertEquals(PaymentStatus.REVIEW, reviewed.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, reviewed.getStatus());
        assertEquals(2, product.getStockQuantity());
    }

    private Product supply(String name, int stock, long price) {
        Product product = new Product(name, null, BigDecimal.valueOf(price), null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, stock);
        return products.saveAndFlush(product);
    }
}
