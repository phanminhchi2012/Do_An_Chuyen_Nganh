package com.example.scalys;

import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderService;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductStockSafetyHttpTests {
    @Autowired MockMvc http;
    @Autowired ProductRepository products;
    @Autowired OrderService orders;

    @Test
    void staleAdminFormCannotRestoreStockReservedByCustomer() throws Exception {
        Product product = new Product("Stock safety test", null, BigDecimal.valueOf(160000),
                null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 20);
        long id = products.saveAndFlush(product).getId();

        var admin = User.withUsername("stock-test-admin").password("unused").roles("ADMIN").build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                admin, null, admin.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            var csrfResponse = http.perform(get("/api/admin/auth/csrf").session(session))
                    .andExpect(status().isOk()).andReturn();
            var matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(csrfResponse.getResponse().getContentAsString());
            if (!matcher.find()) throw new AssertionError("Missing CSRF token");
            String token = matcher.group(1);

            orders.createOrder(new OrderRequest(id, "Khách thử", "0912345678", null,
                    FulfillmentMethod.PICKUP, null, 5, null, null, false, false,
                    "stock@example.test"));
            assertEquals(15, products.findById(id).orElseThrow().getStockQuantity());

            http.perform(patch("/api/admin/products/{id}/stock", id).session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(stockRequest(20, 25)))
                    .andExpect(status().isConflict());
            http.perform(put("/api/admin/products/{id}", id).session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(productUpdate(20, 20)))
                    .andExpect(status().isConflict());
            assertEquals(15, products.findById(id).orElseThrow().getStockQuantity());

            http.perform(put("/api/admin/products/{id}", id).session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(productUpdate(15, 999)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(15));
            http.perform(patch("/api/admin/products/{id}/stock", id).session(session)
                    .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                    .content(stockRequest(15, 12)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(12));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String stockRequest(int expected, int next) {
        return "{\"expectedKind\":\"SUPPLY\",\"expectedStatus\":\"AVAILABLE\","
                + "\"expectedStockQuantity\":" + expected + ",\"stockQuantity\":" + next + "}";
    }

    private String productUpdate(int expected, int submittedStock) {
        return "{\"expectedKind\":\"SUPPLY\",\"expectedStatus\":\"AVAILABLE\","
                + "\"expectedStockQuantity\":" + expected + ",\"product\":{"
                + "\"name\":\"Stock safety test updated\",\"kind\":\"SUPPLY\","
                + "\"category\":\"LIGHTING\",\"stockQuantity\":" + submittedStock + ","
                + "\"price\":160000,\"status\":\"AVAILABLE\"}}";
    }
}
