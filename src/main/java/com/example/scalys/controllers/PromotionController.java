package com.example.scalys.controllers;

import com.example.scalys.dtos.CustomerPromotionResponse;
import com.example.scalys.dtos.PromotionQuoteRequest;
import com.example.scalys.dtos.PromotionQuoteResponse;
import com.example.scalys.services.CustomerSessionService;
import com.example.scalys.services.PromotionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PromotionController {
    private final PromotionService promotions;
    private final CustomerSessionService sessions;

    public PromotionController(PromotionService promotions, CustomerSessionService sessions) {
        this.promotions = promotions;
        this.sessions = sessions;
    }

    @GetMapping("/account/promotions")
    public List<CustomerPromotionResponse> myCodes(HttpServletRequest request) {
        return promotions.myCodes(sessions.required(request).getId());
    }

    @PostMapping("/promotions/quote")
    public PromotionQuoteResponse quote(@Valid @RequestBody PromotionQuoteRequest request,
                                        HttpServletRequest httpRequest) {
        var account = sessions.optional(httpRequest);
        return promotions.quote(request, account == null ? null : account.getId());
    }
}
