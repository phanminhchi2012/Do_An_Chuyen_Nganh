package com.example.scalys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_code_id")
    private CustomerPromotionCode promotionCode;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_method", nullable = false, length = 20)
    private FulfillmentMethod fulfillmentMethod;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType = OrderType.PURCHASE;

    @Column(name = "deposit_consent_at")
    private Instant depositConsentAt;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.PAY_LATER;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.NONE;

    @Column(name = "payment_reference", length = 32, unique = true)
    private String paymentReference;

    @Column(name = "checkout_group", length = 32)
    private String checkoutGroup;

    @Column(name = "public_code", nullable = false, length = 10, unique = true)
    private String publicCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_method", length = 20)
    private BalanceMethod balanceMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_status", nullable = false, length = 20)
    private PaymentStatus balanceStatus = PaymentStatus.NONE;

    @Column(name = "balance_reference", length = 32, unique = true)
    private String balanceReference;

    @Column(name = "balance_expires_at")
    private Instant balanceExpiresAt;

    @Column(name = "balance_paid_at")
    private Instant balancePaidAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "payment_expires_at")
    private Instant paymentExpiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "vnp_transaction_no", length = 30)
    private String vnpTransactionNo;

    @Column(name = "zalopay_transaction_id", length = 30)
    private String zalopayTransactionId;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {
    }

    public Order(Product product, String customerName, String phone, String address,
                 FulfillmentMethod fulfillmentMethod, String note, int quantity, LocalDate requestedDate) {
        this.product = product;
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.fulfillmentMethod = fulfillmentMethod;
        this.note = note;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.paymentAmount = getTotalPrice();
        this.requestedDate = requestedDate;
        this.status = OrderStatus.NEW;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public CustomerAccount getCustomerAccount() { return customerAccount; }
    public void setCustomerAccount(CustomerAccount customerAccount) { this.customerAccount = customerAccount; }
    public CustomerPromotionCode getPromotionCode() { return promotionCode; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void applyPromotion(CustomerPromotionCode code, BigDecimal amount) {
        this.promotionCode = code;
        this.discountAmount = amount;
        this.paymentAmount = getTotalPrice();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() {
        return address;
    }

    public FulfillmentMethod getFulfillmentMethod() {
        return fulfillmentMethod;
    }

    public String getNote() {
        return note;
    }

    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    public BigDecimal getTotalPrice() { return getSubtotal().subtract(discountAmount); }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public OrderType getOrderType() { return orderType; }
    public Instant getDepositConsentAt() { return depositConsentAt; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public void beginDeposit(Instant consentAt) {
        this.orderType = OrderType.HOLD_DEPOSIT;
        this.depositConsentAt = consentAt;
        this.paymentAmount = getTotalPrice().multiply(BigDecimal.valueOf(0.10))
                .setScale(0, java.math.RoundingMode.HALF_UP);
    }
    public void activateDeposit(Instant paidAt) {
        this.holdExpiresAt = paidAt.plus(7, java.time.temporal.ChronoUnit.DAYS);
    }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getPaymentReference() { return paymentReference; }
    public String getPublicCode() { return publicCode; }
    public void setPublicCode(String publicCode) { this.publicCode = publicCode; }
    public BalanceMethod getBalanceMethod() { return balanceMethod; }
    public PaymentStatus getBalanceStatus() { return balanceStatus; }
    public String getBalanceReference() { return balanceReference; }
    public Instant getBalanceExpiresAt() { return balanceExpiresAt; }
    public Instant getBalancePaidAt() { return balancePaidAt; }
    public Instant getClosedAt() { return closedAt; }
    public BigDecimal getBalanceAmount() { return getTotalPrice().subtract(paymentAmount); }
    public void chooseOfflineBalance(BalanceMethod method, Instant at) {
        this.balanceMethod = method;
        this.balanceStatus = PaymentStatus.PENDING;
        this.closedAt = at;
    }
    public void beginOnlineBalance(String reference, Instant expiresAt) {
        this.balanceMethod = BalanceMethod.VNPAY;
        this.balanceStatus = PaymentStatus.PENDING;
        this.balanceReference = reference;
        this.balanceExpiresAt = expiresAt;
    }
    public void markBalancePaid(Instant at) {
        this.balanceStatus = PaymentStatus.PAID;
        this.balancePaidAt = at;
        this.closedAt = at;
    }
    public void setBalanceStatus(PaymentStatus status) { this.balanceStatus = status; }
    public String getCheckoutGroup() { return checkoutGroup; }
    public void setCheckoutGroup(String checkoutGroup) { this.checkoutGroup = checkoutGroup; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public Instant getPaymentExpiresAt() { return paymentExpiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getVnpTransactionNo() { return vnpTransactionNo; }
    public String getZalopayTransactionId() { return zalopayTransactionId; }

    public void beginVnpayPayment(String reference, Instant expiresAt) {
        this.paymentMethod = PaymentMethod.VNPAY;
        beginOnlinePayment(reference, expiresAt);
    }

    public void beginZalopayPayment(String reference, Instant expiresAt) {
        this.paymentMethod = PaymentMethod.ZALOPAY;
        beginOnlinePayment(reference, expiresAt);
    }

    private void beginOnlinePayment(String reference, Instant expiresAt) {
        this.paymentStatus = PaymentStatus.PENDING;
        this.paymentReference = reference;
        this.paymentExpiresAt = expiresAt;
    }

    public void setPaymentStatus(PaymentStatus status) { this.paymentStatus = status; }
    public void recordVnpayPayment(String transactionNo, Instant time) {
        this.vnpTransactionNo = transactionNo;
        this.paidAt = time;
    }
    public void recordVnpayTransaction(String transactionNo) { this.vnpTransactionNo = transactionNo; }
    public void recordZalopayPayment(String transactionId, Instant time) {
        this.zalopayTransactionId = transactionId;
        this.paidAt = time;
    }
    public LocalDate getRequestedDate() { return requestedDate; }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
