package com.example.scalys;

import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CartOrderIntegrationTests {
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void insufficientSecondItemRollsBackFirstReservationAndAllOrders() {
        Product first = supply("Cart test first", 2);
        Product second = supply("Cart test second", 1);
        long orderCount = orders.count();
        try {
            CartOrderRequest request = new CartOrderRequest("Khách thử", "0912345678", null,
                    FulfillmentMethod.PICKUP, null, List.of(
                    new CartOrderRequest.Item(first.getId(), 1),
                    new CartOrderRequest.Item(second.getId(), 2)), "khach@example.test");

            assertThrows(ResponseStatusException.class, () -> service.createCartOrders(request));
            assertEquals(2, products.findById(first.getId()).orElseThrow().getStockQuantity());
            assertEquals(1, products.findById(second.getId()).orElseThrow().getStockQuantity());
            assertEquals(orderCount, orders.count());
        } finally {
            products.deleteById(first.getId());
            products.deleteById(second.getId());
        }
    }

    @Test
    void petAddedToCartIsHeldOnlyAfterCheckout() {
        Product pet = new Product("Cart test pet", "Rắn", BigDecimal.valueOf(500000),
                null, null, ProductStatus.AVAILABLE);
        pet.updateCatalog(ProductKind.PET, null, 1);
        products.saveAndFlush(pet);
        Long orderId = null;
        try {
            assertEquals(ProductStatus.AVAILABLE, products.findById(pet.getId()).orElseThrow().getStatus());
            CartOrderRequest request = new CartOrderRequest("Khách thử", "0912345678", null,
                    FulfillmentMethod.PICKUP, null, List.of(new CartOrderRequest.Item(pet.getId(), 1)),
                    "khach@example.test");

            var created = service.createCartOrders(request);
            orderId = created.getFirst().id();
            assertEquals(1, created.size());
            assertEquals(ProductStatus.HELD, products.findById(pet.getId()).orElseThrow().getStatus());
        } finally {
            if (orderId != null) {
                jdbc.update("DELETE FROM email_outbox WHERE order_id = ?", orderId);
                orders.deleteById(orderId);
            }
            products.deleteById(pet.getId());
        }
    }

    private Product supply(String name, int stock) {
        Product product = new Product(name, null, BigDecimal.valueOf(100000), null, null, ProductStatus.AVAILABLE);
        product.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, stock);
        return products.saveAndFlush(product);
    }
}
