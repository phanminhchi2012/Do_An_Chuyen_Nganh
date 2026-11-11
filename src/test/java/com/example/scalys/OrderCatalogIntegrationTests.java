package com.example.scalys;

import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductCategory;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.services.OrderService;
import com.example.scalys.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OrderCatalogIntegrationTests {
    @Autowired ProductRepository products;
    @Autowired OrderService orders;
    @Autowired ProductService catalog;

    @Test
    void publicCatalogHidesUnlistedAndOutOfStockProductsButAdminCanSeeThem() {
        Product empty = new Product("Test empty stock", null, BigDecimal.valueOf(100000), null, null, ProductStatus.AVAILABLE);
        empty.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 0);
        Long emptyId = products.saveAndFlush(empty).getId();

        Product hidden = new Product("Test hidden stock", null, BigDecimal.valueOf(100000), null, null, ProductStatus.SOLD);
        hidden.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 5);
        Long hiddenId = products.saveAndFlush(hidden).getId();

        Product available = new Product("Test available stock", null, BigDecimal.valueOf(100000), null, null, ProductStatus.AVAILABLE);
        available.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 2);
        Long availableId = products.saveAndFlush(available).getId();

        assertFalse(catalog.getProducts().stream().anyMatch(item -> item.id().equals(emptyId) || item.id().equals(hiddenId)));
        assertTrue(catalog.getProducts().stream().anyMatch(item -> item.id().equals(availableId)));
        assertThrows(ResponseStatusException.class, () -> catalog.getProduct(emptyId));
        assertThrows(ResponseStatusException.class, () -> catalog.getProduct(hiddenId));
        assertTrue(catalog.getAllProducts().stream().anyMatch(item -> item.id().equals(emptyId) || item.id().equals(hiddenId)));
    }

    @Test
    void suppliesReserveQuantityAndCancellationRestoresIt() {
        Product supply = new Product("Test stock", null, BigDecimal.valueOf(100000), null, null, ProductStatus.AVAILABLE);
        supply.updateCatalog(ProductKind.SUPPLY, ProductCategory.LIGHTING, 3);
        Product savedSupply = products.saveAndFlush(supply);

        Long first = orders.createOrder(new OrderRequest(savedSupply.getId(), "Khách thử", "0912345678", null,
                FulfillmentMethod.PICKUP, null, 2, null, null, false, false, "khach@example.test")).id();
        assertEquals(1, savedSupply.getStockQuantity());
        assertEquals(ProductStatus.AVAILABLE, savedSupply.getStatus());
        assertThrows(ResponseStatusException.class, () -> orders.createOrder(new OrderRequest(
                savedSupply.getId(), "Khách thử", "0912345678", null, FulfillmentMethod.PICKUP, null,
                2, null, null, false, false, "khach@example.test")));

        orders.updateStatus(first, OrderStatus.CANCELLED);
        assertEquals(3, savedSupply.getStockQuantity());
    }

    @Test
    void serviceAcceptsSeparateRequestsWithoutHoldingListing() {
        Product service = new Product("Test care", null, BigDecimal.valueOf(120000), null, null, ProductStatus.AVAILABLE);
        service.updateCatalog(ProductKind.SERVICE, ProductCategory.PET_CARE, null);
        service = products.saveAndFlush(service);
        OrderRequest request = new OrderRequest(service.getId(), "Khách thử", "0912345678", null,
                FulfillmentMethod.SERVICE, "Tắc kè", 1, LocalDate.now().plusDays(1), null,
                false, false, "khach@example.test");

        orders.createOrder(request);
        orders.createOrder(request);
        assertEquals(ProductStatus.AVAILABLE, service.getStatus());
    }
}
