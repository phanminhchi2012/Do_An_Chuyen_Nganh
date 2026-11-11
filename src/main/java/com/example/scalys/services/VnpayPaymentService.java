package com.example.scalys.services;

import com.example.scalys.dtos.PaymentStatusResponse;
import com.example.scalys.dtos.VnpayReturnResponse;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.repositories.VnpayIpnEventRepository;
import com.example.scalys.models.VnpayIpnEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class VnpayPaymentService {
    private final VnpayGateway gateway;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final VnpayIpnEventRepository events;
    private final EmailNotificationService notifications;
    private final PromotionService promotions;

    public VnpayPaymentService(VnpayGateway gateway, OrderRepository orders, ProductRepository products,
                               VnpayIpnEventRepository events, EmailNotificationService notifications,
                               PromotionService promotions) {
        this.gateway = gateway;
        this.orders = orders;
        this.products = products;
        this.events = events;
        this.notifications = notifications;
        this.promotions = promotions;
    }

    public boolean isAvailable() { return gateway.isConfigured(); }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String reference) {
        Order initial = orders.findByPaymentReference(reference).orElse(null);
        if (initial != null && initial.getPaymentMethod() == com.example.scalys.models.PaymentMethod.VNPAY)
            return PaymentStatusResponse.from(initial);
        return PaymentStatusResponse.fromBalance(orders.findByBalanceReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch.")));
    }

    @Transactional
    public VnpayReturnResponse verifyReturn(Map<String, String> fields) {
        if (!gateway.validCallback(fields)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chữ ký VNPAY không hợp lệ.");
        }
        Order order = orders.findByPaymentReference(fields.get("vnp_TxnRef")).orElse(null);
        if (order != null && order.getPaymentMethod() != com.example.scalys.models.PaymentMethod.VNPAY)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch.");
        boolean balance = order == null;
        if (balance) order = orders.findByBalanceReference(fields.get("vnp_TxnRef"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch."));
        if (!(balance ? balanceAmountMatches(order, fields) : amountMatches(order, fields))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền VNPAY không khớp đơn hàng.");
        }
        // A signed browser return can complete a sandbox transaction only on the local dev profile.
        // Live environments continue to wait for the server-to-server IPN.
        if (gateway.isLocalReturnConfirmEnabled()) processCallback(fields, "LOCAL_RETURN");
        return new VnpayReturnResponse(balance ? order.getBalanceReference() : order.getPaymentReference(),
                success(fields));
    }

    @Transactional
    public Map<String, String> handleIpn(Map<String, String> fields) {
        return processCallback(fields, "IPN");
    }

    private Map<String, String> processCallback(Map<String, String> fields, String source) {
        if (!gateway.validCallback(fields)) return reply("97", "Invalid signature");
        events.save(new VnpayIpnEvent(fields, source));
        Order balanceOrder = orders.findByBalanceReferenceForUpdate(fields.get("vnp_TxnRef")).orElse(null);
        if (balanceOrder != null) return handleBalanceIpn(balanceOrder, fields);
        Order order = orders.findByPaymentReferenceForUpdate(fields.get("vnp_TxnRef")).orElse(null);
        if (order == null || order.getPaymentMethod() != com.example.scalys.models.PaymentMethod.VNPAY)
            return reply("01", "Order not found");
        if (order.getCheckoutGroup() != null && !order.getCheckoutGroup().equals(order.getPaymentReference()))
            return reply("01", "Order not found");
        if (!amountMatches(order, fields)) return reply("04", "Invalid amount");
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.REVIEW) {
            return reply("02", "Order already confirmed");
        }

        String responseCode = fields.get("vnp_ResponseCode");
        String transactionStatus = fields.get("vnp_TransactionStatus");
        if (responseCode == null || !responseCode.matches("[0-9]{2}")
                || transactionStatus == null || !transactionStatus.matches("[0-9]{2}")) {
            return reply("99", "Missing transaction status");
        }

        boolean successful = success(fields);
        if (successful && (fields.get("vnp_TransactionNo") == null
                || !fields.get("vnp_TransactionNo").matches("[0-9]{1,30}"))) {
            return reply("99", "Missing transaction number");
        }
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            List<Order> lines = paymentLines(order);
            if (successful && order.getStatus() == OrderStatus.NEW) {
                Instant paidAt = Instant.now();
                if (paidAt.isAfter(order.getPaymentExpiresAt())) {
                    for (Order line : lines) {
                        line.setPaymentStatus(PaymentStatus.REVIEW);
                        line.recordVnpayPayment(fields.get("vnp_TransactionNo"), paidAt);
                        cancelAndRelease(line);
                    }
                    return reply("00", "Recorded for review");
                }
                for (Order line : lines) {
                    line.setPaymentStatus(PaymentStatus.PAID);
                    line.recordVnpayPayment(fields.get("vnp_TransactionNo"), paidAt);
                    if (line.getOrderType() == OrderType.HOLD_DEPOSIT) {
                        line.activateDeposit(paidAt);
                    } else {
                        Product product = products.findByIdForUpdate(line.getProduct().getId())
                                .orElseThrow(() -> new IllegalStateException("Product for payment order is missing"));
                        line.setStatus(OrderStatus.CONFIRMED);
                        if (product.getKind() == ProductKind.PET && product.getStockQuantity() == 0)
                            product.setStatus(ProductStatus.SOLD);
                    }
                    notifications.queuePaid(line);
                }
            } else if (successful) {
                for (Order line : lines) {
                    line.setPaymentStatus(PaymentStatus.REVIEW);
                    line.recordVnpayPayment(fields.get("vnp_TransactionNo"), Instant.now());
                }
            } else if ("07".equals(responseCode) || "04".equals(transactionStatus)) {
                for (Order line : lines) {
                    line.setPaymentStatus(PaymentStatus.REVIEW);
                    line.recordVnpayTransaction(fields.get("vnp_TransactionNo"));
                }
            } else {
                for (Order line : lines) {
                    line.setPaymentStatus(PaymentStatus.FAILED);
                    cancelAndRelease(line);
                }
            }
            promotions.settleForOrder(order);
            return reply("00", "Confirm Success");
        }
        if (successful && (order.getPaymentStatus() == PaymentStatus.EXPIRED
                || order.getPaymentStatus() == PaymentStatus.FAILED)) {
            for (Order line : paymentLines(order)) {
                line.setPaymentStatus(PaymentStatus.REVIEW);
                line.recordVnpayPayment(fields.get("vnp_TransactionNo"), Instant.now());
            }
            return reply("00", "Recorded for review");
        }
        return reply("02", "Order already confirmed");
    }

    @Transactional(readOnly = true)
    public List<Long> expiredPendingOrderIds() {
        return orders.findByPaymentStatusAndPaymentExpiresAtBefore(PaymentStatus.PENDING,
                        Instant.now())
                .stream().filter(order -> order.getPaymentMethod() == com.example.scalys.models.PaymentMethod.VNPAY)
                .filter(order -> order.getCheckoutGroup() == null
                        || order.getCheckoutGroup().equals(order.getPaymentReference()))
                .map(Order::getId).toList();
    }

    @Transactional
    public void expirePending(Long id) {
        Order order = orders.findByIdForUpdate(id).orElse(null);
        if (order == null || order.getPaymentStatus() != PaymentStatus.PENDING
                || !order.getPaymentExpiresAt().isBefore(Instant.now())) return;
        for (Order line : paymentLines(order)) {
            line.setPaymentStatus(PaymentStatus.EXPIRED);
            cancelAndRelease(line);
        }
        promotions.settleForOrder(order);
    }

    @Transactional(readOnly = true)
    public List<Long> expiredBalanceIds() {
        return orders.findByBalanceStatusAndBalanceExpiresAtBefore(PaymentStatus.PENDING, Instant.now())
                .stream().filter(order -> order.getBalanceMethod() == com.example.scalys.models.BalanceMethod.VNPAY)
                .map(Order::getId).toList();
    }

    @Transactional
    public void expireBalance(Long id) {
        Order order = orders.findByIdForUpdate(id).orElse(null);
        if (order == null || order.getBalanceMethod() != com.example.scalys.models.BalanceMethod.VNPAY
                || order.getBalanceStatus() != PaymentStatus.PENDING || order.getBalanceExpiresAt() == null
                || order.getBalanceExpiresAt().isAfter(Instant.now())) return;
        order.setBalanceStatus(PaymentStatus.EXPIRED);
    }

    @Transactional(readOnly = true)
    public List<Long> expiredDepositOrderIds() {
        return orders.findByOrderTypeAndStatusAndHoldExpiresAtBefore(
                OrderType.HOLD_DEPOSIT, OrderStatus.NEW, Instant.now())
                .stream().filter(order -> order.getClosedAt() == null
                        && order.getBalanceStatus() != PaymentStatus.PENDING)
                .map(Order::getId).toList();
    }

    @Transactional
    public void expireDeposit(Long id) {
        Order order = orders.findByIdForUpdate(id).orElse(null);
        if (order == null || order.getOrderType() != OrderType.HOLD_DEPOSIT
                || order.getStatus() != OrderStatus.NEW || order.getPaymentStatus() != PaymentStatus.PAID
                || order.getHoldExpiresAt() == null || order.getHoldExpiresAt().isAfter(Instant.now())
                || order.getClosedAt() != null || order.getBalanceStatus() == PaymentStatus.PENDING) return;
        Product product = products.findByIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new IllegalStateException("Product for deposit order is missing"));
        order.setStatus(OrderStatus.HOLD_EXPIRED);
        OrderService.releaseReserved(order, product);
        notifications.queueDepositExpired(order);
    }

    private void cancelAndRelease(Order order) {
        if (order.getStatus() != OrderStatus.NEW) return;
        Product product = products.findByIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new IllegalStateException("Product for payment order is missing"));
        order.setStatus(OrderStatus.CANCELLED);
        OrderService.releaseReserved(order, product);
    }

    private List<Order> paymentLines(Order head) {
        return head.getCheckoutGroup() == null ? List.of(head)
                : orders.findByCheckoutGroupForUpdate(head.getCheckoutGroup());
    }

    private boolean amountMatches(Order order, Map<String, String> fields) {
        return Long.toString(gateway.amountInVnpayUnits(order)).equals(fields.get("vnp_Amount"));
    }

    private boolean balanceAmountMatches(Order order, Map<String, String> fields) {
        return Long.toString(gateway.amountInVnpayUnits(order.getBalanceAmount()))
                .equals(fields.get("vnp_Amount"));
    }

    private Map<String, String> handleBalanceIpn(Order order, Map<String, String> fields) {
        if (!balanceAmountMatches(order, fields)) return reply("04", "Invalid amount");
        if (order.getBalanceStatus() == PaymentStatus.PAID || order.getBalanceStatus() == PaymentStatus.REVIEW)
            return reply("02", "Order already confirmed");
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionStatus = fields.get("vnp_TransactionStatus");
        if (responseCode == null || !responseCode.matches("[0-9]{2}")
                || transactionStatus == null || !transactionStatus.matches("[0-9]{2}"))
            return reply("99", "Missing transaction status");
        boolean successful = success(fields);
        if (successful && (fields.get("vnp_TransactionNo") == null
                || !fields.get("vnp_TransactionNo").matches("[0-9]{1,30}")))
            return reply("99", "Missing transaction number");
        if (successful) {
            if (order.getBalanceStatus() == PaymentStatus.PENDING && order.getStatus() == OrderStatus.NEW
                    && order.getClosedAt() == null && !Instant.now().isAfter(order.getBalanceExpiresAt())) {
                Product product = products.findByIdForUpdate(order.getProduct().getId()).orElseThrow();
                order.markBalancePaid(Instant.now());
                order.setStatus(OrderStatus.CONFIRMED);
                if (product.getKind() == ProductKind.PET && product.getStockQuantity() == 0)
                    product.setStatus(ProductStatus.SOLD);
                notifications.queueBalancePaid(order);
            } else {
                order.setBalanceStatus(PaymentStatus.REVIEW);
            }
            return reply("00", "Confirm Success");
        }
        if (order.getBalanceStatus() == PaymentStatus.PENDING) {
            order.setBalanceStatus("07".equals(responseCode) || "04".equals(transactionStatus)
                    ? PaymentStatus.REVIEW : PaymentStatus.FAILED);
            return reply("00", "Confirm Success");
        }
        return reply("02", "Order already confirmed");
    }

    private boolean success(Map<String, String> fields) {
        return "00".equals(fields.get("vnp_ResponseCode"))
                && "00".equals(fields.get("vnp_TransactionStatus"));
    }

    private Map<String, String> reply(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
