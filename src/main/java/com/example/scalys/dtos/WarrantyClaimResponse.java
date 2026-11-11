package com.example.scalys.dtos;

import com.example.scalys.models.WarrantyClaim;
import java.time.Instant;

public record WarrantyClaimResponse(Long id, String orderCode, String productName, String name,
                                    String phone, String email, String description,
                                    WarrantyClaim.Status status, Instant createdAt,
                                    Instant handledAt, String handledBy) {
    public static WarrantyClaimResponse from(WarrantyClaim claim) {
        return new WarrantyClaimResponse(claim.getId(), claim.getOrderCode(), claim.getProductName(),
                claim.getCustomerName(), claim.getPhone(), claim.getEmail(), claim.getDescription(),
                claim.getStatus(), claim.getCreatedAt(), claim.getHandledAt(), claim.getHandledBy());
    }
}
