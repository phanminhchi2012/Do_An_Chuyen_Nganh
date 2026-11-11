package com.example.scalys.dtos;

import com.example.scalys.models.CustomerAccount;
import java.time.Instant;

public record CustomerAccountResponse(Long id, String email, String fullName, String phone,
                                      String address, Instant createdAt, boolean hasPassword, boolean googleLinked) {
    public static CustomerAccountResponse from(CustomerAccount account) {
        return new CustomerAccountResponse(account.getId(), account.getEmail(), account.getFullName(),
                account.getPhone(), account.getAddress(), account.getCreatedAt(),
                account.getPasswordHash() != null, account.getGoogleSubject() != null);
    }
}
