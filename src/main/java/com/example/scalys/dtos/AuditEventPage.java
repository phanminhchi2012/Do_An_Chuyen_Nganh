package com.example.scalys.dtos;

import java.util.List;

public record AuditEventPage(List<AuditEventResponse> events, long totalEvents,
                             int totalPages, int page) { }
