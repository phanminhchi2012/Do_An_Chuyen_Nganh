package com.example.scalys.services;

import com.example.scalys.dtos.CustomerPromotionResponse;
import com.example.scalys.dtos.PromotionQuoteRequest;
import com.example.scalys.dtos.PromotionQuoteResponse;
import com.example.scalys.dtos.PromotionRequest;
import com.example.scalys.dtos.PromotionResponse;
import com.example.scalys.models.CustomerAccount;
import com.example.scalys.models.CustomerPromotionCode;
import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.Promotion;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.CustomerPromotionCodeRepository;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.repositories.PromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromotionService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PromotionRepository promotions;
    private final CustomerPromotionCodeRepository codes;
    private final CustomerAccountRepository accounts;
    private final ProductRepository products;
    private final OrderRepository orders;

    public PromotionService(PromotionRepository promotions, CustomerPromotionCodeRepository codes,
                            CustomerAccountRepository accounts, ProductRepository products,
                            OrderRepository orders) {
        this.promotions = promotions;
        this.codes = codes;
        this.accounts = accounts;
        this.products = products;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listAdmin() {
        return promotions.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(PromotionResponse::from).toList();
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        String prefix = normalizePrefix(request.codePrefix());
        if (promotions.existsByCodePrefix(prefix))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tiền tố mã khuyến mãi đã tồn tại.");
        validate(request, null);
        return PromotionResponse.from(promotions.save(new Promotion(prefix, request.name().trim(),
                request.discountPercent(), request.minimumTotal(), request.newCustomerOnly(),
                request.enabled(), request.startsAt(), request.endsAt())));
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = promotions.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trình khuyến mãi."));
        if (!promotion.getCodePrefix().equals(normalizePrefix(request.codePrefix())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đổi tiền tố của mã đã phát hành.");
        validate(request, promotion);
        promotion.update(request.name().trim(), request.discountPercent(), request.minimumTotal(),
                request.newCustomerOnly(), request.enabled(), request.startsAt(), request.endsAt());
        return PromotionResponse.from(promotion);
    }

    private String normalizePrefix(String value) {
        String prefix = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!prefix.matches("[A-Z0-9]{3,16}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tiền tố mã chỉ gồm 3–16 chữ cái HOA hoặc số.");
        return prefix;
    }

    private void validate(PromotionRequest request, Promotion existing) {
        if (request.name() == null || request.name().isBlank()
                || request.discountPercent() == null
                || request.discountPercent().compareTo(BigDecimal.ZERO) <= 0
                || request.discountPercent().compareTo(new BigDecimal("50")) > 0
                || request.discountPercent().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức giảm phải lớn hơn 0% và không quá 50%.");
        if (request.minimumTotal() == null || request.minimumTotal().signum() < 0
                || request.minimumTotal().stripTrailingZeros().scale() > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn tối thiểu phải là số đồng không âm.");
        Instant now = Instant.now();
        if (request.startsAt() == null || (existing == null || !existing.getStartsAt().equals(request.startsAt()))
                && request.startsAt().atZone(VIETNAM).toLocalDate().isBefore(LocalDate.now(VIETNAM)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đặt ngày bắt đầu trong quá khứ.");
        if (request.endsAt() != null && (!request.endsAt().isAfter(request.startsAt())
                || (existing == null || !request.endsAt().equals(existing.getEndsAt()))
                    && !request.endsAt().isAfter(now)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu và chưa qua hạn.");
    }

    @Transactional
    public void issueForAccount(CustomerAccount account) {
        issueCodes(account);
    }

    @Transactional
    public List<CustomerPromotionResponse> myCodes(Long accountId) {
        CustomerAccount account = accounts.findByIdForUpdate(accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
        issueCodes(account);
        return codes.findByOwnerIdOrderByIdDesc(accountId).stream()
                .map(code -> CustomerPromotionResponse.from(code, codeStatus(code, account),
                        usedOrderCodes(code, accountId)))
                .toList();
    }

    private List<String> usedOrderCodes(CustomerPromotionCode code, Long accountId) {
        if (code.getUsedAt() == null) return List.of();
        return orders.findByPromotionCodeId(code.getId()).stream()
                .filter(order -> order.getCustomerAccount() != null
                        && accountId.equals(order.getCustomerAccount().getId()))
                .filter(order -> order.getStatus() != OrderStatus.NOT_RECEIVED
                        && (order.getStatus() == OrderStatus.CONFIRMED
                                || order.getPaymentStatus() == PaymentStatus.PAID))
                .sorted(Comparator.comparing(Order::getCreatedAt).thenComparing(Order::getId))
                .map(Order::getPublicCode).distinct().toList();
    }

    private void issueCodes(CustomerAccount account) {
        Instant now = Instant.now();
        for (Promotion promotion : promotions.findByEnabledTrueOrderByIdAsc()) {
            if (promotion.getEndsAt() != null && !promotion.getEndsAt().isAfter(now)) continue;
            if (codes.existsByPromotionIdAndOwnerId(promotion.getId(), account.getId())) continue;
            codes.save(new CustomerPromotionCode(promotion, account, newCode(promotion.getCodePrefix())));
        }
    }

    private String newCode(String prefix) {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder value = new StringBuilder(prefix).append('-');
            for (int index = 0; index < 10; index++)
                value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            if (!codes.existsByCode(value.toString())) return value.toString();
        }
        throw new IllegalStateException("Không tạo được mã khuyến mãi duy nhất.");
    }

    private String codeStatus(CustomerPromotionCode code, CustomerAccount account) {
        Promotion promotion = code.getPromotion();
        Instant now = Instant.now();
        if (code.getUsedAt() != null) return "USED";
        if (code.getReservedGroup() != null) return "RESERVED";
        if (!promotion.isEnabled()) return "DISABLED";
        if (promotion.getEndsAt() != null && !promotion.getEndsAt().isAfter(now)) return "EXPIRED";
        if (promotion.getStartsAt().isAfter(now)) return "UPCOMING";
        if (promotion.isNewCustomerOnly() && account.getFirstPurchaseCompletedAt() != null) return "INELIGIBLE";
        return "AVAILABLE";
    }

    @Transactional
    public PromotionQuoteResponse quote(PromotionQuoteRequest request, Long accountId) {
        Set<Long> seen = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PromotionQuoteRequest.Item item : request.items()) {
            if (!seen.add(item.productId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm bị lặp trong hóa đơn.");
            var product = products.findById(item.productId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
            if (product.getKind() == ProductKind.SERVICE)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dịch vụ không dùng mã khuyến mãi mua hàng.");
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        Selected selected = select(request.promotionCodes(), accountId, subtotal);
        BigDecimal discount = selected == null ? BigDecimal.ZERO : selected.amount();
        return new PromotionQuoteResponse(subtotal, discount, subtotal.subtract(discount),
                selected == null ? null : selected.code().getCode(),
                selected == null ? null : selected.code().getPromotion().getDiscountPercent());
    }

    @Transactional
    public void applyToPurchase(List<Order> lines, Long accountId, List<String> submittedCodes,
                                String reservationGroup, Instant reservedUntil) {
        if (lines.isEmpty()) return;
        BigDecimal subtotal = lines.stream().map(Order::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Selected selected = select(submittedCodes, accountId, subtotal);
        if (selected == null) return;
        CustomerPromotionCode code = selected.code();
        code.reserve(reservationGroup, reservedUntil);
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            Order line = lines.get(index);
            BigDecimal share = selected.amount().multiply(line.getSubtotal())
                    .divide(subtotal, 0, RoundingMode.DOWN);
            shares.add(share);
            allocated = allocated.add(share);
        }
        List<Integer> remainderOrder = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) remainderOrder.add(index);
        remainderOrder.sort(Comparator.<Integer, BigDecimal>comparing(index ->
                selected.amount().multiply(lines.get(index).getSubtotal())
                        .divide(subtotal, 8, RoundingMode.HALF_UP).subtract(shares.get(index)))
                .reversed().thenComparingInt(Integer::intValue));
        for (int index : remainderOrder) {
            if (allocated.compareTo(selected.amount()) >= 0) break;
            shares.set(index, shares.get(index).add(BigDecimal.ONE));
            allocated = allocated.add(BigDecimal.ONE);
        }
        for (int index = 0; index < lines.size(); index++)
            lines.get(index).applyPromotion(code, shares.get(index));
    }

    private record Selected(CustomerPromotionCode code, BigDecimal amount) {}

    private Selected select(List<String> submittedCodes, Long accountId, BigDecimal subtotal) {
        if (submittedCodes == null || submittedCodes.isEmpty()) return null;
        if (submittedCodes.size() > 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được thử tối đa 5 mã cho một hóa đơn.");
        if (accountId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để dùng mã khuyến mãi.");
        CustomerAccount account = accounts.findByIdForUpdate(accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
        Set<String> unique = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String input : submittedCodes) {
            String code = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z0-9]{3,16}-[A-Z2-9]{10}"))
                throw invalidCode();
            if (unique.add(code)) normalized.add(code);
        }
        normalized.sort(String::compareTo);
        Selected best = null;
        for (String value : normalized) {
            CustomerPromotionCode code = codes.findByCodeForUpdate(value).orElseThrow(this::invalidCode);
            settleCode(code);
            if (!code.getOwner().getId().equals(accountId)) throw invalidCode();
            Promotion promotion = code.getPromotion();
            if (!"AVAILABLE".equals(codeStatus(code, account))
                    || subtotal.compareTo(promotion.getMinimumTotal()) < 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mã " + value + " chưa đủ điều kiện, đã dùng hoặc đang chờ đơn khác.");
            BigDecimal amount = subtotal.multiply(promotion.getDiscountPercent())
                    .divide(HUNDRED, 0, RoundingMode.HALF_UP);
            if (best == null || amount.compareTo(best.amount()) > 0) best = new Selected(code, amount);
        }
        return best;
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mã khuyến mãi không hợp lệ hoặc không thuộc tài khoản này.");
    }

    @Transactional
    public void settleForOrder(Order order) {
        if (order.getStatus() == OrderStatus.NOT_RECEIVED) {
            if (order.getCustomerAccount() != null) {
                CustomerAccount account = accounts.findByIdForUpdate(order.getCustomerAccount().getId())
                        .orElseThrow();
                if (!orders.existsSuccessfulPurchaseByAccountId(account.getId()))
                    account.restoreFirstPurchaseEligibility();
            }
            if (order.getPromotionCode() != null) {
                CustomerPromotionCode code = codes.findByCodeForUpdate(order.getPromotionCode().getCode())
                        .orElseThrow();
                if (orders.findByPromotionCodeId(code.getId()).stream().noneMatch(linked ->
                        linked.getStatus() == OrderStatus.CONFIRMED
                                || linked.getPaymentStatus() == PaymentStatus.PAID)) code.restore();
            }
            return;
        }
        boolean successful = order.getOrderType() == OrderType.PURCHASE
                && (order.getStatus() == OrderStatus.CONFIRMED || order.getPaymentStatus() == PaymentStatus.PAID);
        if (successful && order.getCustomerAccount() != null) {
            CustomerAccount account = accounts.findByIdForUpdate(order.getCustomerAccount().getId()).orElseThrow();
            account.completeFirstPurchase(Instant.now());
        }
        if (order.getPromotionCode() != null) {
            CustomerPromotionCode code = codes.findByCodeForUpdate(order.getPromotionCode().getCode()).orElseThrow();
            settleCode(code);
        }
    }

    private void settleCode(CustomerPromotionCode code) {
        if (code.getUsedAt() != null || code.getReservedGroup() == null) return;
        List<Order> linked = orders.findByPromotionCodeId(code.getId());
        if (linked.stream().anyMatch(order -> order.getStatus() == OrderStatus.CONFIRMED
                || order.getPaymentStatus() == PaymentStatus.PAID)) {
            code.redeem(Instant.now());
        } else if (!linked.isEmpty() && linked.stream().noneMatch(order ->
                order.getStatus() == OrderStatus.NEW || order.getPaymentStatus() == PaymentStatus.REVIEW)) {
            code.release();
        }
    }
}
