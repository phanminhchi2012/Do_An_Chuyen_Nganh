package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_promotion_codes")
public class CustomerPromotionCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private CustomerAccount owner;
    @Column(nullable = false, length = 32, unique = true)
    private String code;
    @Column(name = "reserved_group", length = 32)
    private String reservedGroup;
    @Column(name = "reserved_until")
    private Instant reservedUntil;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerPromotionCode() {}

    public CustomerPromotionCode(Promotion promotion, CustomerAccount owner, String code) {
        this.promotion = promotion;
        this.owner = owner;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public void reserve(String group, Instant until) {
        this.reservedGroup = group;
        this.reservedUntil = until;
    }

    public void release() {
        this.reservedGroup = null;
        this.reservedUntil = null;
    }

    public void redeem(Instant at) {
        this.usedAt = at;
        release();
    }

    public void restore() {
        this.usedAt = null;
        release();
    }

    public Long getId() { return id; }
    public Promotion getPromotion() { return promotion; }
    public CustomerAccount getOwner() { return owner; }
    public String getCode() { return code; }
    public String getReservedGroup() { return reservedGroup; }
    public Instant getReservedUntil() { return reservedUntil; }
    public Instant getUsedAt() { return usedAt; }
}
