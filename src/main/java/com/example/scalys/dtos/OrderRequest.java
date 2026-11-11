package com.example.scalys.dtos;

import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotNull @Positive Long productId,
        @NotBlank(message = "Vui lòng nhập tên khách.") @Size(max = 150) String customerName,
        @NotBlank(message = "Vui lòng nhập số điện thoại.")
        @Pattern(regexp = "^(?:0|\\+84)[35789][0-9]{8}$",
                message = "Số điện thoại Việt Nam phải có 10 số, bắt đầu bằng 03/05/07/08/09, hoặc dùng +84.")
        String phone,
        @Size(max = 500) String address,
        FulfillmentMethod fulfillmentMethod,
        String note,
        @Min(1) Integer quantity,
        LocalDate requestedDate,
        PaymentMethod paymentMethod,
        Boolean holdPet,
        Boolean depositConsent,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 5) List<String> promotionCodes
) {
    public OrderRequest {
        if (phone != null) {
            phone = phone.replaceAll("[\\s.-]", "");
        }
    }

    public OrderRequest(Long productId, String customerName, String phone, String address,
                        FulfillmentMethod fulfillmentMethod, String note, Integer quantity,
                        LocalDate requestedDate, PaymentMethod paymentMethod) {
        this(productId, customerName, phone, address, fulfillmentMethod, note,
                quantity, requestedDate, paymentMethod, false, false, null, null);
    }

    public OrderRequest(Long productId, String customerName, String phone, String address,
                        FulfillmentMethod fulfillmentMethod, String note, Integer quantity,
                        LocalDate requestedDate, PaymentMethod paymentMethod,
                        Boolean holdPet, Boolean depositConsent, String email) {
        this(productId, customerName, phone, address, fulfillmentMethod, note,
                quantity, requestedDate, paymentMethod, holdPet, depositConsent, email, null);
    }
}
