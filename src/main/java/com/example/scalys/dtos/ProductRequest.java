package com.example.scalys.dtos;

import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.SpeciesGroup;
import com.example.scalys.models.ProductGender;
import com.example.scalys.models.ProductOrigin;
import com.example.scalys.models.CareDifficulty;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 120) String species,
        ProductKind kind,
        ProductCategory category,
        Integer stockQuantity,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        String description,
        @Size(max = 500) String imageUrl,
        @NotNull ProductStatus status,
        @Size(max = 150) String scientificName,
        SpeciesGroup speciesGroup,
        ProductGender gender,
        LocalDate birthDate,
        @Size(max = 100) String approximateAge,
        @Size(max = 120) String currentSize,
        @Size(max = 120) String adultSize,
        ProductOrigin origin,
        CareDifficulty careDifficulty,
        @Size(max = 100) String individualCode,
        @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+", message = "Link video phải bắt đầu bằng http:// hoặc https://") String videoUrl,
        @Size(max = 30) List<@NotBlank @Size(max = 500) @Pattern(regexp = "(?:https?://[^\\s]+|/api/product-images/[a-f0-9-]{36}\\.(?:png|jpg))") String> imageUrls
) {
    @AssertTrue(message = "Chỉ nhập ngày sinh hoặc tuổi ước tính")
    public boolean isBirthInformationValid() {
        return birthDate == null || approximateAge == null || approximateAge.isBlank();
    }

    @AssertTrue(message = "Ngày sinh không được ở tương lai")
    public boolean isBirthDateValid() {
        return birthDate == null || !birthDate.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "Pet cần loài; hàng hóa cần danh mục và tồn kho; dịch vụ cần danh mục phù hợp")
    public boolean isCatalogValid() {
        ProductKind type = kind == null ? ProductKind.PET : kind;
        if (type != ProductKind.PET && status == ProductStatus.HELD) return false;
        return switch (type) {
            case PET -> species != null && !species.isBlank() && category == null && stockQuantity != null && stockQuantity >= 0;
            case SUPPLY -> category != null && category.supports(type) && stockQuantity != null && stockQuantity >= 0;
            case SERVICE -> category != null && category.supports(type) && stockQuantity == null;
        };
    }
}
