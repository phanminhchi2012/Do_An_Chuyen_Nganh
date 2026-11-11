package com.example.scalys.dtos;

import java.util.List;

public record ContactPageResponse(List<ContactResponse> content, long totalElements,
                                  int totalPages, int page) { }
