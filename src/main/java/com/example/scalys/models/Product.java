package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 120)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductKind kind = ProductKind.PET;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ProductCategory category;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(name = "species_group", length = 30)
    private SpeciesGroup speciesGroup = SpeciesGroup.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductGender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "approximate_age", length = 100)
    private String approximateAge;

    @Column(name = "current_size", length = 120)
    private String currentSize;

    @Column(name = "adult_size", length = 120)
    private String adultSize;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_difficulty", length = 20)
    private CareDifficulty careDifficulty;

    @Column(name = "individual_code", length = 100)
    private String individualCode;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    protected Product() {
    }

    public Product(String name, String species, BigDecimal price, String description,
                   String imageUrl, ProductStatus status) {
        update(name, species, price, description, imageUrl, status);
    }

    public void update(String name, String species, BigDecimal price, String description,
                       String imageUrl, ProductStatus status) {
        this.name = name;
        this.species = species;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public void updateCatalog(ProductKind kind, ProductCategory category, Integer stockQuantity) {
        this.kind = kind;
        this.category = category;
        this.stockQuantity = stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public void updateDetails(String scientificName, SpeciesGroup speciesGroup, ProductGender gender,
                              LocalDate birthDate, String approximateAge, String currentSize, String adultSize,
                              ProductOrigin origin, CareDifficulty careDifficulty, String individualCode,
                              String videoUrl, List<String> imageUrls) {
        this.scientificName = scientificName;
        this.speciesGroup = speciesGroup;
        this.gender = gender;
        this.birthDate = birthDate;
        this.approximateAge = approximateAge;
        this.currentSize = currentSize;
        this.adultSize = adultSize;
        this.origin = origin;
        this.careDifficulty = careDifficulty;
        this.individualCode = individualCode;
        this.videoUrl = videoUrl;
        this.imageUrls.clear();
        this.imageUrls.addAll(imageUrls);
        this.imageUrl = imageUrls.isEmpty() ? null : imageUrls.getFirst();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public ProductKind getKind() { return kind; }
    public ProductCategory getCategory() { return category; }
    public Integer getStockQuantity() { return stockQuantity; }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getScientificName() { return scientificName; }
    public SpeciesGroup getSpeciesGroup() { return speciesGroup == null ? SpeciesGroup.OTHER : speciesGroup; }
    public ProductGender getGender() { return gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getApproximateAge() { return approximateAge; }
    public String getCurrentSize() { return currentSize; }
    public String getAdultSize() { return adultSize; }
    public ProductOrigin getOrigin() { return origin; }
    public CareDifficulty getCareDifficulty() { return careDifficulty; }
    public String getIndividualCode() { return individualCode; }
    public String getVideoUrl() { return videoUrl; }
    public List<String> getImageUrls() { return imageUrls; }
}
