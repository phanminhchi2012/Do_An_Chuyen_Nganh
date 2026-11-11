package com.example.scalys;

import com.example.scalys.services.ZalopayGateway;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZalopayGatewayTests {
    @Test
    void acceptsPublicHttpsReturnForSandboxDemoButRejectsPublicHttpAndLoopbackInProd() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(gateway("https://demo.example.com/orders/zalopay-result", environment, mapper).isConfigured());
        assertFalse(gateway("http://demo.example.com/orders/zalopay-result", environment, mapper).isConfigured());
        assertFalse(gateway("http://localhost:2003/orders/zalopay-result", environment, mapper).isConfigured());
        assertFalse(gateway("https://localhost/orders/zalopay-result", environment, mapper).isConfigured());
        assertFalse(gateway("https://demo.example.com/other", environment, mapper).isConfigured());
    }

    private ZalopayGateway gateway(String returnUrl, MockEnvironment environment, ObjectMapper mapper) {
        return new ZalopayGateway("123", "test-key-one", "test-key-two", returnUrl, environment, mapper);
    }

    @Test
    void acceptsEmvQrReturnedByPublicSandboxApp() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        ObjectMapper mapper = new ObjectMapper();
        ZalopayGateway gateway = new ZalopayGateway("123", "test-key-one", "test-key-two",
                "http://localhost:2003/orders/zalopay-result", environment, mapper);
        String qr = "000201" + "1234567890".repeat(15) + "SHOP.TEST.1234";
        var reply = mapper.readTree("{\"return_code\":1,\"order_url\":\"" + qr
                + "\",\"qr_code\":\"" + qr + "\"}");
        var checkout = gateway.parseCreateReply(reply);
        assertNull(checkout.checkoutUrl());
        assertEquals(qr, checkout.qrCode());
    }

    @Test
    void onlyAValidSignedRedirectIsAccepted() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        ZalopayGateway gateway = new ZalopayGateway("123", "test-key-one", "test-key-two",
                "http://localhost:2003/orders/zalopay-result", environment, new ObjectMapper());
        assertTrue(gateway.isConfigured());
        Map<String, String> fields = new HashMap<>(Map.of(
                "appid", "123", "apptransid", "261023_1234567890abcdef12345678",
                "pmcid", "38", "bankcode", "", "amount", "100000",
                "discountamount", "0", "status", "1"));
        String input = "123|261023_1234567890abcdef12345678|38||100000|0|1";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-key-two".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        fields.put("checksum", HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8))));
        assertTrue(gateway.validRedirect(fields));
        fields.put("amount", "200000");
        assertFalse(gateway.validRedirect(fields));
    }
}
