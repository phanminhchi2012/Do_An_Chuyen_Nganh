package com.example.scalys.controllers;

import com.example.scalys.services.WarrantyClaimService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/warranty-claims")
public class WarrantyClaimController {
    private final WarrantyClaimService claims;
    public WarrantyClaimController(WarrantyClaimService claims) { this.claims = claims; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@RequestParam String orderCode, @RequestParam String name,
                                      @RequestParam String phone, @RequestParam String email,
                                      @RequestParam String description, @RequestParam MultipartFile video,
                                      HttpServletRequest request) {
        Long id = claims.submit(orderCode, name, phone, email, description, video, request.getRemoteAddr());
        return Map.of("id", id, "message", "Shop đã nhận yêu cầu và sẽ liên hệ qua thông tin bạn cung cấp.");
    }
}
