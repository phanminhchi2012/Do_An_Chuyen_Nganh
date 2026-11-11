package com.example.scalys;

import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.WarrantyClaim;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.WarrantyClaimService;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WarrantyClaimIntegrationTests {
    @Autowired MockMvc http;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderService orderService;
    @Autowired WarrantyClaimService claims;

    @Test
    void requiresMatchingOrderAndRealVideoBeforeAdminCanHandleRequest() throws Exception {
        Product product = new Product("Đèn kiểm tra bảo hành", null, BigDecimal.valueOf(100000),
                null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 2);
        product = products.saveAndFlush(product);
        Long orderId = orderService.createOrder(new OrderRequest(product.getId(), "Khách thử", "0912345678",
                null, FulfillmentMethod.PICKUP, null, 1, null, null, false, false,
                "khach@example.test")).id();
        String code = orderRepository.findById(orderId).orElseThrow().getPublicCode();
        byte[] mp4 = {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        var video = new MockMultipartFile("video", "opening.mp4", "video/mp4", mp4);

        http.perform(get("/api/admin/warranty-claims/new-count")).andExpect(status().isUnauthorized());
        http.perform(multipart("/api/warranty-claims").file(video).param("orderCode", code)
                .param("name", "Khách thử").param("phone", "0912345678")
                .param("email", "khach@example.test").param("description", "Đèn không sáng khi mở hộp."))
                .andExpect(status().isForbidden());
        var csrfResponse = http.perform(get("/api/admin/auth/csrf")).andExpect(status().isOk()).andReturn();
        var matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(csrfResponse.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        MockHttpSession session = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        http.perform(multipart("/api/warranty-claims")
                .file(new MockMultipartFile("video", "fake.mp4", "video/mp4", "not a video".getBytes()))
                .param("orderCode", code).param("name", "Khách thử").param("phone", "0912345678")
                .param("email", "khach@example.test").param("description", "Đèn không sáng khi mở hộp.")
                .session(session).header("X-CSRF-TOKEN", matcher.group(1)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());

        assertThrows(ResponseStatusException.class, () -> claims.submit(code, "Khách thử", "0912345679",
                "khach@example.test", "Đèn không sáng khi mở hộp.", video, "test-wrong-phone"));
        assertThrows(ResponseStatusException.class, () -> claims.submit(code, "Khách thử", "0912345678",
                "khach@example.test", "Đèn không sáng khi mở hộp.",
                new MockMultipartFile("video", "fake.mp4", "video/mp4", "not a video".getBytes()),
                "test-fake-video"));

        var upload = http.perform(multipart("/api/warranty-claims").file(video)
                .param("orderCode", code).param("name", "Khách thử").param("phone", "0912345678")
                .param("email", "khach@example.test").param("description", "Đèn không sáng khi mở hộp.")
                .session(session).header("X-CSRF-TOKEN", matcher.group(1)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber()).andReturn();
        var idMatch = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(upload.getResponse().getContentAsString());
        assertThat(idMatch.find()).isTrue();
        Long id = Long.valueOf(idMatch.group(1));
        assertThat(claims.newCount()).isGreaterThan(0);
        assertThat(claims.video(id).resource().exists()).isTrue();
        assertThat(claims.list(WarrantyClaim.Status.NEW, 0).content()).extracting("id").contains(id);
        http.perform(get("/api/admin/warranty-claims/{id}/video", id)).andExpect(status().isUnauthorized());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin-test", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var adminSession = new MockHttpSession();
        adminSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        try {
            http.perform(get("/api/admin/warranty-claims").param("status", "NEW").session(adminSession))
                    .andExpect(status().isOk());
            http.perform(get("/api/admin/warranty-claims/{id}/video", id).session(adminSession))
                    .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.valueOf("video/mp4")));
            var adminCsrf = http.perform(get("/api/admin/auth/csrf").session(adminSession))
                    .andExpect(status().isOk()).andReturn();
            var adminToken = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(adminCsrf.getResponse().getContentAsString());
            assertThat(adminToken.find()).isTrue();
            http.perform(patch("/api/admin/warranty-claims/{id}/status", id).session(adminSession)
                    .header("X-CSRF-TOKEN", adminToken.group(1))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDLED\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.handledBy").value("admin-test"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
