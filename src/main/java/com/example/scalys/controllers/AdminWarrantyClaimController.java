package com.example.scalys.controllers;

import com.example.scalys.dtos.WarrantyClaimPage;
import com.example.scalys.dtos.WarrantyClaimResponse;
import com.example.scalys.models.WarrantyClaim;
import com.example.scalys.services.WarrantyClaimService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/warranty-claims")
public class AdminWarrantyClaimController {
    private final WarrantyClaimService claims;
    public AdminWarrantyClaimController(WarrantyClaimService claims) { this.claims = claims; }

    @GetMapping
    public WarrantyClaimPage list(@RequestParam(required = false) WarrantyClaim.Status status,
                                  @RequestParam(defaultValue = "0") int page) {
        return claims.list(status, page);
    }

    @GetMapping("/new-count")
    public Map<String, Long> newCount() { return Map.of("count", claims.newCount()); }

    @GetMapping("/{id}/video")
    public ResponseEntity<Resource> video(@PathVariable Long id) {
        var file = claims.video(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.length())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    @PatchMapping("/{id}/status")
    public WarrantyClaimResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatus request,
                                              Authentication authentication) {
        return claims.updateStatus(id, request.status(), authentication.getName());
    }

    public record UpdateStatus(@NotNull WarrantyClaim.Status status) { }
}
