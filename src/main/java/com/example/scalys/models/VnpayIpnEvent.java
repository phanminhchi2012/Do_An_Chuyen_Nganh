package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "vnpay_ipn_events")
public class VnpayIpnEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String reference;

    @Column(name = "transaction_no", length = 30)
    private String transactionNo;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "response_code", length = 2)
    private String responseCode;

    @Column(name = "transaction_status", length = 2)
    private String transactionStatus;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected VnpayIpnEvent() { }

    public VnpayIpnEvent(Map<String, String> fields, String source) {
        this.reference = cut(fields.get("vnp_TxnRef"), 32);
        this.transactionNo = cut(fields.get("vnp_TransactionNo"), 30);
        try { this.amount = Long.parseLong(fields.getOrDefault("vnp_Amount", "0")); }
        catch (NumberFormatException ignored) { this.amount = 0L; }
        this.responseCode = cut(fields.get("vnp_ResponseCode"), 2);
        this.transactionStatus = cut(fields.get("vnp_TransactionStatus"), 2);
        this.source = source;
        this.receivedAt = Instant.now();
    }

    private String cut(String value, int length) {
        return value == null ? null : value.substring(0, Math.min(value.length(), length));
    }
}
