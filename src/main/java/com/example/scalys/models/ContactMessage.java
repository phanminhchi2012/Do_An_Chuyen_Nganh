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
@Table(name = "contact_messages")
public class ContactMessage {
    public enum Status { NEW, HANDLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;
    @Column(nullable = false, length = 20)
    private String phone;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, columnDefinition = "text")
    private String question;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.NEW;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "handled_at")
    private Instant handledAt;
    @Column(name = "handled_by", length = 254)
    private String handledBy;

    protected ContactMessage() { }

    public ContactMessage(String customerName, String phone, String email, String question) {
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.question = question;
    }

    public void setStatus(Status next, String actor) {
        status = next;
        handledAt = next == Status.HANDLED ? Instant.now() : null;
        handledBy = next == Status.HANDLED ? actor : null;
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getQuestion() { return question; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getHandledAt() { return handledAt; }
    public String getHandledBy() { return handledBy; }
}
