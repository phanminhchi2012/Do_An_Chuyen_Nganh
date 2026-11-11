package com.example.scalys.services;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ZalopayGateway {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CREATE_URL = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String QUERY_URL = "https://sb-openapi.zalopay.vn/v2/query";
    private final String appId;
    private final String key1;
    private final String key2;
    private final String returnUrl;
    private final boolean devProfile;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final SecureRandom random = new SecureRandom();

    public ZalopayGateway(@Value("${zalopay.app-id:}") String appId,
                          @Value("${zalopay.key1:}") String key1,
                          @Value("${zalopay.key2:}") String key2,
                          @Value("${zalopay.return-url:}") String returnUrl,
                          Environment environment, ObjectMapper mapper) {
        this.appId = appId.trim();
        this.key1 = key1.trim();
        this.key2 = key2.trim();
        this.returnUrl = returnUrl.trim();
        this.devProfile = environment.acceptsProfiles(Profiles.of("dev"));
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        try {
            URI uri = URI.create(returnUrl);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !"/orders/zalopay-result".equals(uri.getPath()))
                return false;
            boolean loopback = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            boolean localDev = devProfile && loopback && "http".equalsIgnoreCase(uri.getScheme())
                    && uri.getPort() == 2003;
            boolean publicHttps = !loopback && "https".equalsIgnoreCase(uri.getScheme())
                    && (uri.getPort() == -1 || uri.getPort() == 443);
            return appId.matches("[1-9][0-9]*") && !key1.isBlank() && !key2.isBlank()
                    && (localDev || publicHttps);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public void requireConfigured() {
        if (!isConfigured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "ZaloPay sandbox chưa được cấu hình trên máy chủ.");
    }

    public long amountInDong(BigDecimal amount) {
        try {
            long dong = amount.longValueExact();
            if (dong <= 0) throw new ArithmeticException("unsupported amount");
            return dong;
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giá trị đơn hàng không phù hợp để thanh toán ZaloPay.");
        }
    }

    public String newReference() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return LocalDate.now(VIETNAM).format(DATE) + "_" + HexFormat.of().formatHex(bytes);
    }

    public CheckoutData createCheckout(String reference, BigDecimal amount) {
        requireConfigured();
        long appTime = System.currentTimeMillis();
        String item = "[]";
        String embedData;
        try {
            embedData = mapper.writeValueAsString(Map.of("redirecturl", returnUrl));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize ZaloPay return URL", exception);
        }
        long dong = amountInDong(amount);
        String appUser = "scalys";
        String input = String.join("|", appId, reference, appUser, Long.toString(dong),
                Long.toString(appTime), embedData, item);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("app_id", Integer.parseInt(appId));
        request.put("app_user", appUser);
        request.put("app_trans_id", reference);
        request.put("app_time", appTime);
        request.put("expire_duration_seconds", 900);
        request.put("amount", dong);
        request.put("description", "Thanh toan don hang Scalys " + reference);
        request.put("bank_code", "");
        request.put("item", item);
        request.put("embed_data", embedData);
        request.put("mac", hmac(key1, input));
        return parseCreateReply(post(CREATE_URL, request));
    }

    public CheckoutData parseCreateReply(JsonNode reply) {
        String orderUrl = reply.path("order_url").asText("");
        String qrCode = reply.path("qr_code").asText("");
        if (reply.path("return_code").asInt() != 1)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "ZaloPay chưa tạo được phiên thanh toán. Vui lòng thử lại.");
        if (validCheckoutUrl(orderUrl)) return new CheckoutData(orderUrl, null);
        // The shared sandbox AppID currently returns EMV QR content in order_url.
        if (validQrCode(qrCode) && (orderUrl.isBlank() || orderUrl.equals(qrCode)))
            return new CheckoutData(null, qrCode);
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "ZaloPay không trả về liên kết hoặc mã QR thanh toán hợp lệ.");
    }

    public QueryResult query(String reference) {
        requireConfigured();
        Map<String, Object> request = Map.of("app_id", Integer.parseInt(appId),
                "app_trans_id", reference, "mac", hmac(key1, appId + "|" + reference + "|" + key1));
        JsonNode reply = post(QUERY_URL, request);
        return new QueryResult(reply.path("return_code").asInt(0), reply.path("amount").asLong(-1),
                reply.path("zp_trans_id").asText(""), reply.path("server_time").asLong(0));
    }

    public boolean validRedirect(Map<String, String> fields) {
        if (!isConfigured() || !appId.equals(fields.get("appid"))) return false;
        String reference = fields.get("apptransid");
        String checksum = fields.get("checksum");
        if (reference == null || !reference.matches("[0-9]{6}_[0-9a-f]{24}")
                || checksum == null || !checksum.matches("[0-9a-fA-F]{64}")) return false;
        for (String name : new String[] {"pmcid", "bankcode", "amount", "discountamount", "status"})
            if (!fields.containsKey(name)) return false;
        String input = String.join("|", appId, reference, fields.get("pmcid"), fields.get("bankcode"),
                fields.get("amount"), fields.get("discountamount"), fields.get("status"));
        return MessageDigest.isEqual(hmac(key2, input).getBytes(StandardCharsets.US_ASCII),
                checksum.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private JsonNode post(String url, Map<String, Object> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("ZaloPay HTTP " + response.statusCode());
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kết nối ZaloPay bị gián đoạn.");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Chưa kết nối được ZaloPay sandbox. Vui lòng thử lại.");
        }
    }

    private boolean validCheckoutUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.equals("zalopay.vn") || host.endsWith(".zalopay.vn"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean validQrCode(String value) {
        return value != null && value.length() >= 40 && value.length() <= 2048
                && value.startsWith("000201")
                && value.chars().allMatch(character -> character >= 0x20 && character <= 0x7e);
    }

    private String hmac(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign ZaloPay request", exception);
        }
    }

    public record QueryResult(int returnCode, long amount, String transactionId, long serverTime) { }
    public record CheckoutData(String checkoutUrl, String qrCode) { }
}
