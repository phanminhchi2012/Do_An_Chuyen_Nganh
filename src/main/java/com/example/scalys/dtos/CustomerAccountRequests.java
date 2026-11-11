package com.example.scalys.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CustomerAccountRequests {
    private CustomerAccountRequests() {}

    public record Register(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^(?:0|\\+84)[35789][0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ.") String phone,
            @NotBlank(message = "Vui lòng nhập địa chỉ.") @Size(max = 500) String address,
            @NotBlank String password) {}

    public record Login(@NotBlank String email, @NotBlank String password) {}

    public record Address(@NotBlank(message = "Vui lòng nhập địa chỉ.") @Size(max = 500) String address) {}

    public record Password(@NotBlank String currentPassword, @NotBlank String newPassword) {}

}
