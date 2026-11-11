package com.example.scalys.dtos;

import com.example.scalys.models.ContactMessage;
import java.time.Instant;

public record ContactResponse(Long id, String name, String phone, String email, String question,
                              ContactMessage.Status status, Instant createdAt,
                              Instant handledAt, String handledBy) {
    public static ContactResponse from(ContactMessage message) {
        return new ContactResponse(message.getId(), message.getCustomerName(), message.getPhone(),
                message.getEmail(), message.getQuestion(), message.getStatus(),
                message.getCreatedAt(), message.getHandledAt(), message.getHandledBy());
    }
}
