package com.example.scalys.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminUserRequests {
    private AdminUserRequests() {}
    public record CreateStaff(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(max = 150) String fullName,
                              @NotBlank String password) {}
    public record UpdateStaff(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(max = 150) String fullName,
                              @NotNull Boolean enabled, String newPassword) {}
    public record UpdateCustomer(@NotBlank @Email @Size(max = 254) String email,
                                 @NotBlank @Size(max = 150) String fullName,
                                 @NotBlank @Pattern(regexp = "^(?:0|\\+84)[35789][0-9]{8}$") String phone,
                                 @NotBlank @Size(max = 500) String address,
                                 @NotNull Boolean enabled) {}
}
