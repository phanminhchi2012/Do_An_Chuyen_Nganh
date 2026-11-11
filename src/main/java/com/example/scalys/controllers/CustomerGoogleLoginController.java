package com.example.scalys.controllers;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/google")
public class CustomerGoogleLoginController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public CustomerGoogleLoginController(ObjectProvider<ClientRegistrationRepository> registrations) {
        this.registrations = registrations;
    }

    @GetMapping("/availability")
    public Map<String, Boolean> availability() {
        return Map.of("available", registrations.getIfAvailable() != null);
    }
}
