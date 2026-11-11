package com.example.scalys.services;

import com.example.scalys.dtos.AdminOrderResponse;
import com.example.scalys.dtos.CloseHoldRequest;
import com.example.scalys.dtos.OrderLookupRequest;
import com.example.scalys.dtos.OrderLookupResponse;
import com.example.scalys.models.BalanceMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OrderFollowupService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final VnpayGateway gateway;
    private final OrderLookupLimiter limiter;
    private final EmailNotificationService notifications;
    private final PromotionService promotions;

    public OrderFollowupService(OrderRepository orders, ProductRepository products,
                                VnpayGateway gateway, OrderLookupLimiter limiter,
                                EmailNotificationService notifications, PromotionService promotions) {
        this.orders = orders;
        this.products = products;
        this.gateway = gateway;
        this.limiter = limiter;
        this.notifications = notifications;
        this.promotions = promotions;
    }

    @Transactional(readOnly = true)
    public OrderLookupResponse lookup(OrderLookupRequest request, String ip) {
        return OrderLookupResponse.from(findPublic(request.code(), request.phone(), ip, false), null);
    }

    @Transactional
    public OrderLookupResponse closeHold(CloseHoldRequest request, String ip) {
        Order order = findPublic(request.code(), request.phone(), ip, true);
        ensureOpenDeposit(order);
        BalanceMethod method = request.balanceMethod();
        if (method == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Vui lòng chọn cách thanh toán 90% còn lại.");
        Instant now = Instant.now();
        if (method == BalanceMethod.VNPAY) {
            if (order.getBalanceReference() != null)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Lần thanh toán VNPay trước chưa đối soát xong. Vui lòng liên hệ shop.");
            gateway.requireConfigured();
            gateway.amountInVnpayUnits(order.getBalanceAmount());
            String reference = UUID.randomUUID().toString().replace("-", "");
            order.beginOnlineBalance(reference, now.plus(15, ChronoUnit.MINUTES));
            String checkoutUrl = gateway.createCheckoutUrl(reference, order.getBalanceAmount(), now,
                    order.getBalanceExpiresAt(), order.getId(), ip);
            return OrderLookupResponse.from(order, checkoutUrl);
        }
        order.chooseOfflineBalance(method, now);
        return OrderLookupResponse.from(order, null);
    }

    @Transactional
    public OrderLookupResponse cancelHold(OrderLookupRequest request, String ip) {
        Order order = findPublic(request.code(), request.phone(), ip, true);
        ensureOpenDeposit(order);
        Product product = products.findByIdForUpdate(order.getProduct().getId()).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        OrderService.releaseReserved(order, product);
        promotions.settleForOrder(order);
        return OrderLookupResponse.from(order, null);
    }

    @Transactional
    public AdminOrderResponse confirmBalance(Long id) {
        Order order = orders.findByIdForUpdate(id).orElseThrow(() -> notFound());
        if (order.getOrderType() != OrderType.HOLD_DEPOSIT || order.getStatus() != OrderStatus.NEW
                || order.getPaymentStatus() != PaymentStatus.PAID || order.getClosedAt() == null
                || (order.getBalanceMethod() != BalanceMethod.COD
                    && order.getBalanceMethod() != BalanceMethod.TAI_SHOP)
                || order.getBalanceStatus() != PaymentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn chưa sẵn sàng xác nhận đã nhận đủ tiền.");
        Product product = products.findByIdForUpdate(order.getProduct().getId()).orElseThrow();
        order.markBalancePaid(Instant.now());
        order.setStatus(OrderStatus.CONFIRMED);
        if (product.getKind() == ProductKind.PET && product.getStockQuantity() == 0)
            product.setStatus(ProductStatus.SOLD);
        notifications.queueBalancePaid(order);
        return AdminOrderResponse.from(order);
    }

    @Transactional
    public AdminOrderResponse notReceived(Long id) {
        Order order = orders.findByIdForUpdate(id).orElseThrow(() -> notFound());
        if (order.getStatus() != OrderStatus.CONFIRMED || (order.getPaymentMethod() != PaymentMethod.PAY_LATER
                && order.getBalanceMethod() != BalanceMethod.COD))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không thể đánh dấu khách không nhận.");
        Product product = products.findByIdForUpdate(order.getProduct().getId()).orElseThrow();
        order.setStatus(OrderStatus.NOT_RECEIVED);
        OrderService.releaseReserved(order, product);
        if (product.getKind() == ProductKind.PET) product.setStatus(ProductStatus.AVAILABLE);
        promotions.settleForOrder(order);
        return AdminOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<Long> expiredCodIds() {
        return orders.findByPaymentMethodAndStatusAndCreatedAtBefore(
                PaymentMethod.PAY_LATER, OrderStatus.NEW, Instant.now().minus(24, ChronoUnit.HOURS))
                .stream().filter(order -> order.getOrderType() == OrderType.PURCHASE
                        && order.getProduct().getKind() != ProductKind.SERVICE)
                .map(Order::getId).toList();
    }

    @Transactional
    public void expireCod(Long id) {
        Order order = orders.findByIdForUpdate(id).orElse(null);
        if (order == null || order.getPaymentMethod() != PaymentMethod.PAY_LATER
                || order.getOrderType() != OrderType.PURCHASE || order.getStatus() != OrderStatus.NEW
                || order.getCreatedAt().plus(24, ChronoUnit.HOURS).isAfter(Instant.now())) return;
        Product product = products.findByIdForUpdate(order.getProduct().getId()).orElseThrow();
        if (product.getKind() == ProductKind.SERVICE) return;
        order.setStatus(OrderStatus.CANCELLED);
        OrderService.releaseReserved(order, product);
        promotions.settleForOrder(order);
    }

    private Order findPublic(String code, String phone, String ip, boolean lock) {
        limiter.check(ip);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        String normalizedPhone = phone == null ? "" : phone.replaceAll("[\\s.-]", "");
        Order order = normalizedCode.matches("[A-HJ-NP-Z2-9]{10}")
                && normalizedPhone.matches("^(?:0|\\+84)[35789][0-9]{8}$")
                ? orders.findByPublicCodeAndPhone(normalizedCode, normalizedPhone).orElse(null) : null;
        if (order == null) {
            limiter.fail(ip);
            throw notFound();
        }
        if (lock) return orders.findByIdForUpdate(order.getId()).orElseThrow(this::notFound);
        return order;
    }

    private void ensureOpenDeposit(Order order) {
        if (order.getOrderType() != OrderType.HOLD_DEPOSIT || order.getPaymentStatus() != PaymentStatus.PAID
                || order.getStatus() != OrderStatus.NEW || order.getClosedAt() != null
                || order.getHoldExpiresAt() == null || !order.getHoldExpiresAt().isAfter(Instant.now())
                || order.getBalanceStatus() == PaymentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lượt giữ pet không còn hiệu lực để chốt hoặc huỷ.");
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn");
    }
}
