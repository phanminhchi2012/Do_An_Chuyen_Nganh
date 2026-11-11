package com.example.scalys;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=example-client.apps.googleusercontent.com",
        "spring.security.oauth2.client.registration.google.client-secret=example-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.google.redirect-uri=http://127.0.0.1:2003/login/oauth2/code/google"
})
@AutoConfigureMockMvc
class CustomerGoogleOAuthHttpTests {
    @Autowired MockMvc http;

    @Test
    void configuredClientExposesLoginAndUsesTheFrontendCallback() throws Exception {
        http.perform(get("/api/account/google/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
        String location = http.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        String query = URLDecoder.decode(URI.create(location).getRawQuery(), StandardCharsets.UTF_8);
        assertTrue(query.contains("redirect_uri=http://127.0.0.1:2003/login/oauth2/code/google"));
        assertTrue(query.contains("prompt=select_account"));
    }
}
