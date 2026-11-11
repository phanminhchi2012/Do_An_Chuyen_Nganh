package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotions")
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code_prefix", nullable = false, length = 16, unique = true)
    private String codePrefix;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;
    @Column(name = "minimum_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumTotal;
    @Column(name = "new_customer_only", nullable = false)
    private boolean newCustomerOnly;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Promotion() {}

    public Promotion(String codePrefix, String name, BigDecimal discountPercent,
                     BigDecimal minimumTotal, boolean newCustomerOnly,
                     boolean enabled, Instant startsAt, Instant endsAt) {
        this.codePrefix = codePrefix;
        this.createdAt = Instant.now();
        update(name, discountPercent, minimumTotal, newCustomerOnly, enabled, startsAt, endsAt);
    }

    public void update(String name, BigDecimal discountPercent, BigDecimal minimumTotal,
                       boolean newCustomerOnly, boolean enabled, Instant startsAt, Instant endsAt) {
        this.name = name;
        this.discountPercent = discountPercent;
        this.minimumTotal = minimumTotal;
        this.newCustomerOnly = newCustomerOnly;
        this.enabled = enabled;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Long getId() { return id; }
    public String getCodePrefix() { return codePrefix; }
    public String getName() { return name; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public BigDecimal getMinimumTotal() { return minimumTotal; }
    public boolean isNewCustomerOnly() { return newCustomerOnly; }
    public boolean isEnabled() { return enabled; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
