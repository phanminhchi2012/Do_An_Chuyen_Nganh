package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "staff_accounts")
public class StaffAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "session_version", nullable = false)
    private int sessionVersion;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected StaffAccount() {}
    public StaffAccount(String email, String fullName, String passwordHash) {
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public int getSessionVersion() { return sessionVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void update(String email, String fullName, boolean enabled, String passwordHash) {
        boolean changed = this.enabled != enabled || !this.email.equals(email);
        this.email = email;
        this.fullName = fullName;
        this.enabled = enabled;
        if (passwordHash != null) { this.passwordHash = passwordHash; changed = true; }
        if (changed) sessionVersion++;
    }
    public void loggedIn() { lastLoginAt = Instant.now(); }
}
