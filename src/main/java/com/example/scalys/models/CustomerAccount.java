package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(name = "google_subject", length = 255, unique = true)
    private String googleSubject;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "first_purchase_completed_at")
    private Instant firstPurchaseCompletedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    protected CustomerAccount() {}

    public CustomerAccount(String email, String passwordHash, String fullName, String phone, String address) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.createdAt = Instant.now();
    }

    public static CustomerAccount fromGoogle(String email, String googleSubject, String fullName,
                                             String phone, String address) {
        CustomerAccount account = new CustomerAccount(email, null, fullName, phone, address);
        account.googleSubject = googleSubject;
        return account;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getGoogleSubject() { return googleSubject; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public int getSessionVersion() { return sessionVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFirstPurchaseCompletedAt() { return firstPurchaseCompletedAt; }
    public boolean isEnabled() { return enabled; }

    public void updateByStaff(String fullName, String email, String phone, String address, boolean enabled) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        if (this.enabled != enabled) sessionVersion++;
        this.enabled = enabled;
    }

    public void completeFirstPurchase(Instant at) {
        if (firstPurchaseCompletedAt == null) firstPurchaseCompletedAt = at;
    }

    public void restoreFirstPurchaseEligibility() {
        firstPurchaseCompletedAt = null;
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.sessionVersion++;
    }

    public void linkGoogle(String subject) {
        this.googleSubject = subject;
        this.sessionVersion++;
    }
}
