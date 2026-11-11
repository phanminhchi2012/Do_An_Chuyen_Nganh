package com.example.scalys.dtos;

import java.util.List;

public record WarrantyClaimPage(List<WarrantyClaimResponse> content, long totalElements,
                                int totalPages, int page) { }
