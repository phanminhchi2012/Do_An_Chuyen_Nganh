package com.example.scalys.controllers;

import com.example.scalys.dtos.ProductRequest;
import com.example.scalys.dtos.ProductResponse;
import com.example.scalys.dtos.ProductStatusRequest;
import com.example.scalys.dtos.ProductStockRequest;
import com.example.scalys.dtos.ProductUpdateRequest;
import com.example.scalys.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable("id") Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable("id") Long id, @Valid @RequestBody ProductStockRequest request) {
        return productService.updateStock(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse updateStatus(@PathVariable("id") Long id, @Valid @RequestBody ProductStatusRequest request) {
        return productService.updateStatus(id, request.status());
    }
}
