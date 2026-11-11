package com.example.scalys.dtos;

import java.time.Instant;

public record AuditEventResponse(Long id, String actorRole, String actorId, String action,
                                 String targetType, String targetId, String requestPath,
                                 String ipAddress, Instant occurredAt) {}
