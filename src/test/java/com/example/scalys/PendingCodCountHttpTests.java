package com.example.scalys;

import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PendingCodCountHttpTests {
    @Autowired MockMvc http;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;

    @Test
    void badgeCountsOnlyNewCodPurchasesAndClearsAfterConfirmation() throws Exception {
        http.perform(get("/api/admin/orders/pending-cod-count"))
                .andExpect(status().isUnauthorized());

        var admin = User.withUsername("test-admin").password("unused").roles("ADMIN").build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                admin, null, admin.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        long before = orders.countPendingCodOrders();
        try {
            Product pet = new Product("COD badge pet", "Rắn", BigDecimal.valueOf(200000),
                    null, null, ProductStatus.AVAILABLE);
            pet.updateCatalog(ProductKind.PET, null, 2);
            pet = products.saveAndFlush(pet);
            Order cod = new Order(pet, "Khách COD", "0912345678", null,
                    FulfillmentMethod.PICKUP, null, 1, null);
            cod.setPublicCode(code());
            cod = orders.saveAndFlush(cod);

            Product service = new Product("COD badge service", null, BigDecimal.valueOf(100000),
                    null, null, ProductStatus.AVAILABLE);
            service.updateCatalog(ProductKind.SERVICE, ProductCategory.PET_CARE, null);
            service = products.saveAndFlush(service);
            Order serviceRequest = new Order(service, "Khách dịch vụ", "0912345678", null,
                    FulfillmentMethod.SERVICE, null, 1, null);
            serviceRequest.setPublicCode(code());
            orders.saveAndFlush(serviceRequest);

            Order online = new Order(pet, "Khách online", "0912345678", null,
                    FulfillmentMethod.PICKUP, null, 1, null);
            online.setPublicCode(code());
            online.beginVnpayPayment(UUID.randomUUID().toString().replace("-", ""), Instant.now().plusSeconds(900));
            orders.saveAndFlush(online);

            http.perform(get("/api/admin/orders/pending-cod-count").session(session))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(before + 1));
            cod.setStatus(OrderStatus.CONFIRMED);
            orders.saveAndFlush(cod);
            http.perform(get("/api/admin/orders/pending-cod-count").session(session))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(before));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String code() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
