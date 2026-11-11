package com.example.scalys.dtos;

import com.example.scalys.models.CustomerAccount;
import com.example.scalys.models.StaffAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminUsersResponse(Owner owner, List<Staff> staff, List<Customer> customers) {
    public record Owner(String username, String role, boolean editable) {}
    public record Staff(Long id, String email, String fullName, boolean enabled,
                        Instant createdAt, Instant lastLoginAt) {
        public static Staff from(StaffAccount account) {
            return new Staff(account.getId(), account.getEmail(), account.getFullName(),
                    account.isEnabled(), account.getCreatedAt(), account.getLastLoginAt());
        }
    }
    public record Customer(Long id, String email, String fullName, String phone, String address,
                           boolean enabled, Instant createdAt, BigDecimal totalPetSpent) {
        public static Customer from(CustomerAccount account, BigDecimal totalPetSpent) {
            return new Customer(account.getId(), account.getEmail(), account.getFullName(),
                    account.getPhone(), account.getAddress(), account.isEnabled(), account.getCreatedAt(),
                    totalPetSpent);
        }
    }
}
