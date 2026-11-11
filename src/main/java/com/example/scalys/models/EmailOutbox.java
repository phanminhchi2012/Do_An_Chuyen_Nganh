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
@Table(name = "email_outbox")
public class EmailOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private ContactMessage contact;

    @Column(nullable = false, length = 30)
    private String kind;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected EmailOutbox() { }

    public EmailOutbox(Order order, String kind, String recipient, String subject, String body) {
        this.order = order;
        this.kind = kind;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public EmailOutbox(ContactMessage contact, String kind, String recipient, String subject, String body) {
        this.contact = contact;
        this.kind = kind;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getSentAt() { return sentAt; }
    public void markSent() { this.sentAt = Instant.now(); }
}
