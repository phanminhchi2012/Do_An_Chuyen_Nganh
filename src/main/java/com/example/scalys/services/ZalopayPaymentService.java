package com.example.scalys.services;

import com.example.scalys.dtos.PaymentStatusResponse;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ZalopayPaymentService {
    private final ZalopayGateway gateway;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final EmailNotificationService notifications;
    private final PromotionService promotions;
    private final TransactionTemplate transactions;

    public ZalopayPaymentService(ZalopayGateway gateway, OrderRepository orders,
                                 ProductRepository products, EmailNotificationService notifications,
                                 PromotionService promotions, TransactionTemplate transactions) {
        this.gateway = gateway;
        this.orders = orders;
        this.products = products;
        this.notifications = notifications;
        this.promotions = promotions;
        this.transactions = transactions;
    }

    public boolean isAvailable() { return gateway.isConfigured(); }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String reference) {
        Order order = head(reference);
        return PaymentStatusResponse.from(order);
    }

    public PaymentStatusResponse verifyReturn(String reference, long amount) {
        Order order = head(reference);
        if (gateway.amountInDong(order.getPaymentAmount()) != amount)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền ZaloPay không khớp đơn hàng.");
        refresh(reference);
        return getStatus(reference);
    }

    public void refresh(String reference) {
        Order snapshot = head(reference);
        if (snapshot.getPaymentStatus() == PaymentStatus.PAID) return;
        ZalopayGateway.QueryResult result;
        try {
            result = gateway.query(reference);
        } catch (RuntimeException exception) {
            if (snapshot.getPaymentExpiresAt().isBefore(Instant.now()))
                transactions.executeWithoutResult(status -> expire(reference));
            throw exception;
        }
        transactions.executeWithoutResult(status -> apply(reference, result));
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void reconcileUnfinished() {
        if (!gateway.isConfigured()) return;
        Instant oldest = Instant.now().minus(1, ChronoUnit.DAYS);
        for (Order order : orders.findByPaymentMethodAndPaymentStatusIn(PaymentMethod.ZALOPAY,
                List.of(PaymentStatus.PENDING, PaymentStatus.EXPIRED))) {
            if (order.getCreatedAt().isBefore(oldest) && order.getPaymentStatus() == PaymentStatus.EXPIRED) continue;
            if (!order.getPaymentReference().matches("[0-9]{6}_[0-9a-f]{24}")) continue;
            try {
                refresh(order.getPaymentReference());
            } catch (RuntimeException ignored) {
                // The next run retries the query; an expired reservation is released in refresh().
            }
        }
    }

    private Order head(String reference) {
        Order order = orders.findByPaymentReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch."));
        if (order.getPaymentMethod() != PaymentMethod.ZALOPAY
                || !reference.matches("[0-9]{6}_[0-9a-f]{24}"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch.");
        return order;
    }

    private void apply(String reference, ZalopayGateway.QueryResult result) {
        Order head = orders.findByPaymentReferenceForUpdate(reference).orElse(null);
        if (head == null || head.getPaymentMethod() != PaymentMethod.ZALOPAY
                || head.getPaymentStatus() == PaymentStatus.PAID) return;
        List<Order> lines = lines(head);
        if (result.returnCode() == 1) {
            String transactionId = result.transactionId();
            if (result.amount() != gateway.amountInDong(head.getPaymentAmount())
                    || transactionId == null || !transactionId.matches("[1-9][0-9]{0,29}")) {
                reviewAndRelease(lines, null, Instant.now());
            } else {
                Instant paidAt = result.serverTime() > 0
                        ? Instant.ofEpochMilli(result.serverTime()) : Instant.now();
                if (head.getPaymentStatus() == PaymentStatus.PENDING && head.getStatus() == OrderStatus.NEW
                        && !paidAt.isAfter(head.getPaymentExpiresAt())) {
                    for (Order line : lines) {
                        line.setPaymentStatus(PaymentStatus.PAID);
                        line.recordZalopayPayment(transactionId, paidAt);
                        line.setStatus(OrderStatus.CONFIRMED);
                        Product product = products.findByIdForUpdate(line.getProduct().getId()).orElseThrow();
                        if (product.getKind() == ProductKind.PET && product.getStockQuantity() == 0)
                            product.setStatus(ProductStatus.SOLD);
                        notifications.queuePaid(line);
                    }
                } else {
                    reviewAndRelease(lines, transactionId, paidAt);
                }
            }
        } else if (result.returnCode() == 2 && head.getPaymentStatus() == PaymentStatus.PENDING) {
            for (Order line : lines) {
                line.setPaymentStatus(PaymentStatus.FAILED);
                cancelAndRelease(line);
            }
        } else if (!head.getPaymentExpiresAt().isAfter(Instant.now())) {
            expireLines(lines);
        }
        promotions.settleForOrder(head);
    }

    private void expire(String reference) {
        Order head = orders.findByPaymentReferenceForUpdate(reference).orElse(null);
        if (head == null || head.getPaymentMethod() != PaymentMethod.ZALOPAY
                || head.getPaymentStatus() != PaymentStatus.PENDING
                || head.getPaymentExpiresAt().isAfter(Instant.now())) return;
        expireLines(lines(head));
        promotions.settleForOrder(head);
    }

    private void expireLines(List<Order> lines) {
        for (Order line : lines) {
            if (line.getPaymentStatus() != PaymentStatus.PENDING) continue;
            line.setPaymentStatus(PaymentStatus.EXPIRED);
            cancelAndRelease(line);
        }
    }

    private void reviewAndRelease(List<Order> lines, String transactionId, Instant paidAt) {
        for (Order line : lines) {
            line.setPaymentStatus(PaymentStatus.REVIEW);
            if (transactionId != null) line.recordZalopayPayment(transactionId, paidAt);
            cancelAndRelease(line);
        }
    }

    private void cancelAndRelease(Order line) {
        if (line.getStatus() != OrderStatus.NEW) return;
        Product product = products.findByIdForUpdate(line.getProduct().getId()).orElseThrow();
        line.setStatus(OrderStatus.CANCELLED);
        OrderService.releaseReserved(line, product);
    }

    private List<Order> lines(Order head) {
        return head.getCheckoutGroup() == null ? List.of(head)
                : orders.findByCheckoutGroupForUpdate(head.getCheckoutGroup());
    }
}
