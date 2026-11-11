package com.example.scalys.controllers;

import com.example.scalys.dtos.PromotionRequest;
import com.example.scalys.dtos.PromotionResponse;
import com.example.scalys.services.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {
    private final PromotionService promotions;

    public AdminPromotionController(PromotionService promotions) { this.promotions = promotions; }

    @GetMapping
    public List<PromotionResponse> list() { return promotions.listAdmin(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse create(@Valid @RequestBody PromotionRequest request) {
        return promotions.create(request);
    }

    @PutMapping("/{id}")
    public PromotionResponse update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return promotions.update(id, request);
    }
}
