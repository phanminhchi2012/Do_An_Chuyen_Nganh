package com.example.scalys.services;

import com.example.scalys.dtos.AdminOrderResponse;
import com.example.scalys.dtos.OrderRequest;
import com.example.scalys.dtos.CartOrderRequest;
import com.example.scalys.dtos.OrderResponse;
import com.example.scalys.models.FulfillmentMethod;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.PaymentMethod;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.CustomerAccount;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.repositories.CustomerAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class OrderService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VnpayGateway vnpayGateway;
    private final ZalopayGateway zalopayGateway;
    private final CustomerAccountRepository customerAccounts;
    private final EmailNotificationService notifications;
    private final PromotionService promotions;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        VnpayGateway vnpayGateway, ZalopayGateway zalopayGateway,
                        CustomerAccountRepository customerAccounts, EmailNotificationService notifications,
                        PromotionService promotions) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.vnpayGateway = vnpayGateway;
        this.zalopayGateway = zalopayGateway;
        this.customerAccounts = customerAccounts;
        this.notifications = notifications;
        this.promotions = promotions;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        return createOrder(request, "127.0.0.1");
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerIp) {
        return createOrder(request, customerIp, null);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerIp, Long customerAccountId) {
        return createOrder(request, customerIp, customerAccountId, true, false);
    }

    @Transactional
    public OrderResponse createSingleZalopayPending(OrderRequest request, Long customerAccountId) {
        if (request.paymentMethod() != PaymentMethod.ZALOPAY || Boolean.TRUE.equals(request.holdPet()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZaloPay chỉ áp dụng khi mua sản phẩm.");
        return createOrder(request, "127.0.0.1", customerAccountId, false, true);
    }

    private OrderResponse createOrder(OrderRequest request, String customerIp,
                                      Long customerAccountId, boolean sendReceipt, boolean allowZalopay) {
        CustomerAccount customerAccount = customerAccountId == null ? null : customerAccounts.findById(customerAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
        String orderEmail = customerAccount == null ? request.email() : customerAccount.getEmail();
        if (orderEmail == null || orderEmail.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập email.");
        boolean holdPet = Boolean.TRUE.equals(request.holdPet());
        FulfillmentMethod fulfillmentMethod = holdPet ? FulfillmentMethod.UNDECIDED : request.fulfillmentMethod();
        if (fulfillmentMethod == null || (!holdPet && fulfillmentMethod == FulfillmentMethod.UNDECIDED))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hình thức nhận.");
        PaymentMethod paymentMethod = request.paymentMethod() == null
                ? (holdPet ? PaymentMethod.VNPAY : PaymentMethod.PAY_LATER) : request.paymentMethod();
        if (paymentMethod == PaymentMethod.ZALOPAY && !allowZalopay)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng dùng luồng thanh toán ZaloPay.");
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));

        ProductKind kind = product.getKind();
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0.");
        }
        if (product.getStatus() == ProductStatus.SOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm đã bán, không thể đặt hàng.");
        }
        if (kind != ProductKind.SERVICE && (product.getStatus() != ProductStatus.AVAILABLE
                || product.getStockQuantity() == null || product.getStockQuantity() < quantity)) {
            int remaining = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Shop hiện chỉ còn " + remaining + " " + (kind == ProductKind.PET ? "pet" : "sản phẩm") + ".");
        }
        if (holdPet && (kind != ProductKind.PET || paymentMethod != PaymentMethod.VNPAY
                || !Boolean.TRUE.equals(request.depositConsent()) || orderEmail.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giữ pet cần email, đồng ý chính sách cọc và thanh toán qua VNPAY.");
        }
        if ((holdPet || kind == ProductKind.SERVICE)
                && request.promotionCodes() != null && !request.promotionCodes().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mã khuyến mãi chỉ áp dụng khi mua pet hoặc sản phẩm, không áp dụng giữ cọc/dịch vụ.");
        if (kind == ProductKind.SERVICE && (fulfillmentMethod != FulfillmentMethod.SERVICE
                || request.requestedDate() == null || request.requestedDate().isBefore(LocalDate.now())
                || quantity != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ngày dịch vụ hợp lệ.");
        }
        if (kind != ProductKind.SERVICE && !holdPet && fulfillmentMethod == FulfillmentMethod.SERVICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hình thức nhận không phù hợp.");
        }
        if (kind == ProductKind.SERVICE && paymentMethod != PaymentMethod.PAY_LATER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dịch vụ cần shop xác nhận giá trước khi thanh toán.");
        }
        if (paymentMethod == PaymentMethod.VNPAY) vnpayGateway.requireConfigured();
        if (paymentMethod == PaymentMethod.ZALOPAY) zalopayGateway.requireConfigured();

        String address = holdPet ? null : blankToNull(request.address());
        if (fulfillmentMethod == FulfillmentMethod.DELIVERY && address == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập địa chỉ giao hàng.");
        }

        Order order = new Order(product,
                customerAccount == null ? request.customerName().trim() : customerAccount.getFullName(),
                customerAccount == null || customerAccount.getPhone() == null
                        ? request.phone() : customerAccount.getPhone(), address,
                fulfillmentMethod, blankToNull(request.note()), quantity,
                kind == ProductKind.SERVICE ? request.requestedDate() : null);
        order.setPublicCode(newPublicCode());
        order.setEmail(blankToNull(orderEmail));
        order.setCustomerAccount(customerAccount);
        if (!holdPet && kind != ProductKind.SERVICE)
            promotions.applyToPurchase(List.of(order), customerAccountId, request.promotionCodes(),
                    order.getPublicCode(), order.getCreatedAt().plus(paymentMethod == PaymentMethod.PAY_LATER
                            ? 24 : 15, paymentMethod == PaymentMethod.PAY_LATER
                            ? ChronoUnit.HOURS : ChronoUnit.MINUTES));
        if (holdPet) order.beginDeposit(Instant.now());
        if (paymentMethod == PaymentMethod.VNPAY) {
            vnpayGateway.amountInVnpayUnits(order);
            order.beginVnpayPayment(UUID.randomUUID().toString().replace("-", ""),
                    order.getCreatedAt().plus(15, ChronoUnit.MINUTES));
        }
        if (paymentMethod == PaymentMethod.ZALOPAY) {
            zalopayGateway.amountInDong(order.getPaymentAmount());
            order.beginZalopayPayment(zalopayGateway.newReference(),
                    order.getCreatedAt().plus(15, ChronoUnit.MINUTES));
        }
        if (kind != ProductKind.SERVICE) {
            product.setStockQuantity(product.getStockQuantity() - quantity);
            if (kind == ProductKind.PET && product.getStockQuantity() == 0) product.setStatus(ProductStatus.HELD);
        }
        orderRepository.save(order);
        if (paymentMethod == PaymentMethod.PAY_LATER && sendReceipt) notifications.queueOrderReceived(order);
        String checkoutUrl = paymentMethod == PaymentMethod.VNPAY
                ? vnpayGateway.createCheckoutUrl(order, customerIp) : null;
        return OrderResponse.from(order, checkoutUrl);
    }

    @Transactional
    public List<OrderResponse> createCartOrders(CartOrderRequest request) {
        return createCartOrders(request, null);
    }

    @Transactional
    public List<OrderResponse> createCartOrders(CartOrderRequest request, Long customerAccountId) {
        return createCartOrders(request, customerAccountId, true);
    }

    private List<OrderResponse> createCartOrders(CartOrderRequest request, Long customerAccountId,
                                                  boolean sendReceipt) {
        if (request.email() == null || request.email().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập email.");
        if (request.fulfillmentMethod() != FulfillmentMethod.DELIVERY
                && request.fulfillmentMethod() != FulfillmentMethod.PICKUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hình thức nhận không phù hợp với giỏ hàng.");
        }
        Set<Long> ids = new HashSet<>();
        for (CartOrderRequest.Item item : request.items()) {
            if (!ids.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm bị lặp trong giỏ hàng.");
            }
        }
        // Lock in the same order for concurrent carts. An error rolls back every stock change.
        List<OrderResponse> created = request.items().stream()
                .sorted(Comparator.comparing(CartOrderRequest.Item::productId))
                .map(item -> {
                    Product product = productRepository.findByIdForUpdate(item.productId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ."));
                    if (product.getKind() != ProductKind.SUPPLY && product.getKind() != ProductKind.PET) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dịch vụ cần gửi yêu cầu riêng.");
                    }
                    return createOrder(new OrderRequest(item.productId(), request.customerName(), request.phone(),
                            request.address(), request.fulfillmentMethod(), request.note(), item.quantity(),
                            null, PaymentMethod.PAY_LATER, false, false, request.email()), "127.0.0.1",
                            customerAccountId, sendReceipt, false);
                })
                .toList();
        String group = UUID.randomUUID().toString().replace("-", "");
        List<Order> lines = created.stream().map(line -> orderRepository.findById(line.id()).orElseThrow()).toList();
        lines.forEach(line -> line.setCheckoutGroup(group));
        promotions.applyToPurchase(lines, customerAccountId, request.promotionCodes(), group,
                Instant.now().plus(sendReceipt ? 24 : 15,
                        sendReceipt ? ChronoUnit.HOURS : ChronoUnit.MINUTES));
        return lines.stream().map(line -> OrderResponse.from(line, null)).toList();
    }

    @Transactional
    public OrderResponse createCartVnpay(CartOrderRequest request, String customerIp) {
        return createCartVnpay(request, customerIp, null);
    }

    @Transactional
    public OrderResponse createCartVnpay(CartOrderRequest request, String customerIp, Long customerAccountId) {
        vnpayGateway.requireConfigured();
        List<OrderResponse> created = createCartOrders(request, customerAccountId, false);
        String group = orderRepository.findById(created.get(0).id()).orElseThrow().getCheckoutGroup();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        BigDecimal total = BigDecimal.ZERO;
        Order head = null;
        for (OrderResponse line : created) {
            Order order = orderRepository.findById(line.id()).orElseThrow();
            if (head == null) head = order;
            total = total.add(order.getTotalPrice());
            order.setCheckoutGroup(group);
            order.beginVnpayPayment(head == order ? group : UUID.randomUUID().toString().replace("-", ""), expiresAt);
        }
        head.setPaymentAmount(total);
        vnpayGateway.amountInVnpayUnits(head);
        return OrderResponse.from(head, vnpayGateway.createCheckoutUrl(head, customerIp));
    }

    @Transactional
    public OrderResponse createCartZalopayPending(CartOrderRequest request, Long customerAccountId) {
        zalopayGateway.requireConfigured();
        List<OrderResponse> created = createCartOrders(request, customerAccountId, false);
        String group = orderRepository.findById(created.get(0).id()).orElseThrow().getCheckoutGroup();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        BigDecimal total = BigDecimal.ZERO;
        Order head = null;
        for (OrderResponse line : created) {
            Order order = orderRepository.findById(line.id()).orElseThrow();
            if (head == null) head = order;
            total = total.add(order.getTotalPrice());
            order.setCheckoutGroup(group);
            order.beginZalopayPayment(head == order ? zalopayGateway.newReference()
                    : UUID.randomUUID().toString().replace("-", ""), expiresAt);
        }
        head.setPaymentAmount(total);
        zalopayGateway.amountInDong(total);
        return OrderResponse.from(head, null);
    }

    @Transactional
    public void cancelUnstartedZalopay(String reference) {
        Order head = orderRepository.findByPaymentReferenceForUpdate(reference).orElse(null);
        if (head == null || head.getPaymentMethod() != PaymentMethod.ZALOPAY
                || head.getPaymentStatus() != PaymentStatus.PENDING || head.getStatus() != OrderStatus.NEW) return;
        List<Order> lines = head.getCheckoutGroup() == null ? List.of(head)
                : orderRepository.findByCheckoutGroupForUpdate(head.getCheckoutGroup());
        for (Order line : lines) {
            if (line.getPaymentStatus() != PaymentStatus.PENDING || line.getStatus() != OrderStatus.NEW) continue;
            Product product = productRepository.findByIdForUpdate(line.getProduct().getId()).orElseThrow();
            line.setPaymentStatus(PaymentStatus.FAILED);
            line.setStatus(OrderStatus.CANCELLED);
            releaseReserved(line, product);
        }
        promotions.settleForOrder(head);
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDescIdDesc()
                : orderRepository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return orders.stream().map(AdminOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countPendingCodOrders() {
        return orderRepository.countPendingCodOrders();
    }

    @Transactional
    public AdminOrderResponse updateStatus(Long id, OrderStatus targetStatus) {
        if (targetStatus != OrderStatus.CONFIRMED && targetStatus != OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xác nhận hoặc huỷ đơn.");
        }

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã được xử lý, không thể thay đổi nữa.");
        }
        if (targetStatus == OrderStatus.CONFIRMED && order.getPaymentMethod() == PaymentMethod.PAY_LATER
                && order.getOrderType() == OrderType.PURCHASE
                && order.getProduct().getKind() != ProductKind.SERVICE
                && !order.getCreatedAt().plus(24, ChronoUnit.HOURS).isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn COD đã quá 24 giờ xác nhận.");
        }
        if (order.getPaymentMethod() == PaymentMethod.VNPAY
                || order.getPaymentMethod() == PaymentMethod.ZALOPAY) {
            if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đang chờ cổng thanh toán xác nhận.");
            }
            if (order.getPaymentStatus() == PaymentStatus.REVIEW) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch cần đối soát thủ công.");
            }
            if (targetStatus == OrderStatus.CONFIRMED && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn chưa thanh toán.");
            }
            if (targetStatus == OrderStatus.CANCELLED && order.getPaymentStatus() == PaymentStatus.PAID
                    && order.getOrderType() != OrderType.HOLD_DEPOSIT) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cần xử lý hoàn tiền trước khi huỷ đơn đã thanh toán.");
            }
        }

        Product product = productRepository.findByIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
        if (order.getOrderType() == OrderType.HOLD_DEPOSIT && targetStatus == OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đơn giữ cọc cần thanh toán đủ phần còn lại trước khi xác nhận.");
        }

        order.setStatus(targetStatus);
        if (targetStatus == OrderStatus.CANCELLED) releaseReserved(order, product);
        else if (product.getKind() == ProductKind.PET && product.getStockQuantity() == 0)
            product.setStatus(ProductStatus.SOLD);
        promotions.settleForOrder(order);
        return AdminOrderResponse.from(order);
    }

    static void releaseReserved(Order order, Product product) {
        if (product.getKind() == ProductKind.PET || product.getKind() == ProductKind.SUPPLY) {
            product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
            if (product.getKind() == ProductKind.PET && product.getStatus() == ProductStatus.HELD)
                product.setStatus(ProductStatus.AVAILABLE);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String newPublicCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(10);
            for (int index = 0; index < 10; index++)
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            if (!orderRepository.existsByPublicCode(code.toString())) return code.toString();
        }
        throw new IllegalStateException("Không tạo được mã đơn duy nhất.");
    }
}
