package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "warranty_claims")
public class WarrantyClaim {
    public enum Status { NEW, HANDLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "order_code", nullable = false, length = 10)
    private String orderCode;
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;
    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;
    @Column(nullable = false, length = 20)
    private String phone;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(name = "video_file", nullable = false, length = 80)
    private String videoFile;
    @Column(name = "video_content_type", nullable = false, length = 40)
    private String videoContentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.NEW;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "handled_at")
    private Instant handledAt;
    @Column(name = "handled_by", length = 254)
    private String handledBy;

    protected WarrantyClaim() { }

    public WarrantyClaim(Long orderId, String orderCode, String productName, String customerName,
                         String phone, String email, String description, String videoFile, String videoContentType) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.productName = productName;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.description = description;
        this.videoFile = videoFile;
        this.videoContentType = videoContentType;
    }

    public void setStatus(Status next, String actor) {
        status = next;
        handledAt = next == Status.HANDLED ? Instant.now() : null;
        handledBy = next == Status.HANDLED ? actor : null;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getOrderCode() { return orderCode; }
    public String getProductName() { return productName; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getDescription() { return description; }
    public String getVideoFile() { return videoFile; }
    public String getVideoContentType() { return videoContentType; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getHandledAt() { return handledAt; }
    public String getHandledBy() { return handledBy; }
}
