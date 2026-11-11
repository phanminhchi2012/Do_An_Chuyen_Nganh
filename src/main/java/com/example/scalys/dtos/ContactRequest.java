package com.example.scalys.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "^(?:0|\\+84)[35789][0-9]{8}$",
                message = "Số điện thoại Việt Nam không hợp lệ.") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 2000) String question
) {
    public ContactRequest {
        if (phone != null) phone = phone.replaceAll("[\\s.-]", "");
    }
}
