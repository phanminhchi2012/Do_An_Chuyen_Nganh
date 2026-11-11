package com.example.scalys.services;

import com.example.scalys.models.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
public class VnpayGateway {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final String ipnUrl;
    private final boolean localReturnConfirm;

    public VnpayGateway(@Value("${vnpay.tmn-code:}") String tmnCode,
                        @Value("${vnpay.hash-secret:}") String hashSecret,
                        @Value("${vnpay.pay-url:}") String payUrl,
                        @Value("${vnpay.return-url:}") String returnUrl,
                        @Value("${vnpay.ipn-url:}") String ipnUrl,
                        @Value("${vnpay.local-return-confirm:false}") boolean localReturnConfirm,
                        Environment environment) {
        this.tmnCode = tmnCode.trim();
        this.hashSecret = hashSecret.trim();
        this.payUrl = payUrl.trim();
        this.returnUrl = returnUrl.trim();
        this.ipnUrl = ipnUrl.trim();
        this.localReturnConfirm = localReturnConfirm
                && environment.acceptsProfiles(Profiles.of("dev"))
                && isLoopbackReturnUrl(this.returnUrl);
    }

    public boolean isConfigured() {
        return tmnCode.matches("[A-Za-z0-9]{8}") && !hashSecret.isBlank()
                && validUrl(payUrl, true) && validUrl(returnUrl, false)
                && (validUrl(ipnUrl, true) || isLocalReturnConfirmEnabled());
    }

    public boolean isLocalReturnConfirmEnabled() {
        return localReturnConfirm && !validUrl(ipnUrl, true);
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Thanh toán VNPAY chưa được cấu hình trên máy chủ.");
        }
    }

    private boolean validUrl(String value, boolean requireHttps) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && !uri.getHost().isBlank()
                    && ("https".equalsIgnoreCase(uri.getScheme())
                        || (!requireHttps && "http".equalsIgnoreCase(uri.getScheme())
                            && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isLoopbackReturnUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))
                    && "/orders/payment-result".equals(uri.getPath());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public long amountInVnpayUnits(Order order) {
        return amountInVnpayUnits(order.getPaymentAmount());
    }

    public long amountInVnpayUnits(BigDecimal amount) {
        try {
            long dong = amount.longValueExact();
            if (dong < 5_000 || dong > 9_999_999_999L) {
                throw new ArithmeticException("unsupported amount");
            }
            return Math.multiplyExact(dong, 100);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giá trị đơn hàng không phù hợp để thanh toán VNPAY.");
        }
    }

    public String createCheckoutUrl(Order order, String customerIp) {
        return createCheckoutUrl(order.getPaymentReference(), order.getPaymentAmount(),
                order.getCreatedAt(), order.getPaymentExpiresAt(), order.getId(), customerIp);
    }

    public String createCheckoutUrl(String reference, BigDecimal amount, Instant createdAt,
                                    Instant expiresAt, Long orderId, String customerIp) {
        requireConfigured();
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", tmnCode);
        fields.put("vnp_Amount", Long.toString(amountInVnpayUnits(amount)));
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_TxnRef", reference);
        fields.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_ReturnUrl", returnUrl);
        fields.put("vnp_IpAddr", customerIp == null || customerIp.isBlank() ? "127.0.0.1" : customerIp);
        fields.put("vnp_CreateDate", formatTime(createdAt));
        fields.put("vnp_ExpireDate", formatTime(expiresAt));
        String signedQuery = canonical(fields);
        return payUrl + "?" + signedQuery + "&vnp_SecureHash=" + hmac(signedQuery);
    }

    public boolean validCallback(Map<String, String> fields) {
        String supplied = fields.get("vnp_SecureHash");
        if (tmnCode.isBlank() || hashSecret.isBlank() || supplied == null || !supplied.matches("(?i)[0-9a-f]{128}")
                || !tmnCode.equals(fields.get("vnp_TmnCode"))) return false;
        Map<String, String> signed = new TreeMap<>(fields);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        byte[] expected = hmac(canonical(signed)).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, supplied.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private String formatTime(Instant instant) {
        return DATE_FORMAT.format(instant.atZone(VIETNAM));
    }

    private String canonical(Map<String, String> fields) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("vnp_") && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không tạo được chữ ký VNPAY.", exception);
        }
    }
}
