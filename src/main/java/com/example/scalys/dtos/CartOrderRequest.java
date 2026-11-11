package com.example.scalys.dtos;

import com.example.scalys.models.FulfillmentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

import java.util.List;

public record CartOrderRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Pattern(regexp = "^(?:0|\\+84)[35789][0-9]{8}$",
                message = "Số điện thoại Việt Nam phải có 10 số hợp lệ.") String phone,
        @Size(max = 500) String address,
        @NotNull FulfillmentMethod fulfillmentMethod,
        String note,
        @NotNull @Size(min = 1, max = 20) List<@Valid Item> items,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 5) List<String> promotionCodes
) {
    public CartOrderRequest {
        if (phone != null) phone = phone.replaceAll("[\\s.-]", "");
    }

    public CartOrderRequest(String customerName, String phone, String address,
                            FulfillmentMethod fulfillmentMethod, String note, List<Item> items) {
        this(customerName, phone, address, fulfillmentMethod, note, items, null, null);
    }

    public CartOrderRequest(String customerName, String phone, String address,
                            FulfillmentMethod fulfillmentMethod, String note, List<Item> items,
                            String email) {
        this(customerName, phone, address, fulfillmentMethod, note, items, email, null);
    }

    public record Item(@NotNull @Positive Long productId, @NotNull @Min(1) Integer quantity) {
    }
}
