package com.example.scalys.dtos;

import com.example.scalys.models.Product;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.SpeciesGroup;
import com.example.scalys.models.ProductGender;
import com.example.scalys.models.ProductOrigin;
import com.example.scalys.models.CareDifficulty;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String species,
        ProductKind kind,
        ProductCategory category,
        String categoryLabel,
        Integer stockQuantity,
        BigDecimal price,
        String description,
        String imageUrl,
        ProductStatus status,
        String statusLabel,
        String scientificName,
        SpeciesGroup speciesGroup,
        String speciesGroupLabel,
        ProductGender gender,
        String genderLabel,
        LocalDate birthDate,
        String approximateAge,
        String currentSize,
        String adultSize,
        ProductOrigin origin,
        String originLabel,
        CareDifficulty careDifficulty,
        String careDifficultyLabel,
        String individualCode,
        String videoUrl,
        List<String> imageUrls
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSpecies(),
                product.getKind(),
                product.getCategory(),
                product.getCategory() == null ? null : product.getCategory().getLabel(),
                product.getStockQuantity(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus(),
                product.getStatus().getLabel(),
                product.getScientificName(),
                product.getSpeciesGroup(),
                product.getSpeciesGroup().getLabel(),
                product.getGender(),
                product.getGender() == null ? null : product.getGender().getLabel(),
                product.getBirthDate(),
                product.getApproximateAge(),
                product.getCurrentSize(),
                product.getAdultSize(),
                product.getOrigin(),
                product.getOrigin() == null ? null : product.getOrigin().getLabel(),
                product.getCareDifficulty(),
                product.getCareDifficulty() == null ? null : product.getCareDifficulty().getLabel(),
                product.getIndividualCode(),
                product.getVideoUrl(),
                List.copyOf(product.getImageUrls())
        );
    }
}
