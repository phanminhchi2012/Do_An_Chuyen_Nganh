package com.example.scalys.controllers;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.CustomerAccountResponse;
import com.example.scalys.dtos.CustomerOrderPage;
import com.example.scalys.models.CustomerAccount;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.CustomerOrderHistoryService;
import com.example.scalys.services.CustomerSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class CustomerAccountController {
    private final CustomerAccountService accounts;
    private final CustomerSessionService sessions;
    private final CustomerOrderHistoryService history;

    public CustomerAccountController(CustomerAccountService accounts, CustomerSessionService sessions,
                                     CustomerOrderHistoryService history) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.history = history;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAccountResponse register(@Valid @RequestBody CustomerAccountRequests.Register request) {
        CustomerAccount account = accounts.register(request);
        return CustomerAccountResponse.from(account);
    }

    @PostMapping("/login")
    public CustomerAccountResponse login(@Valid @RequestBody CustomerAccountRequests.Login request,
                                         HttpServletRequest httpRequest) {
        CustomerAccount account = accounts.login(request, httpRequest.getRemoteAddr());
        sessions.start(httpRequest, account);
        return CustomerAccountResponse.from(account);
    }

    @GetMapping("/me")
    public CustomerAccountResponse me(HttpServletRequest request) {
        return CustomerAccountResponse.from(sessions.required(request));
    }

    @PatchMapping("/me")
    public CustomerAccountResponse update(@Valid @RequestBody CustomerAccountRequests.Address request,
                                          HttpServletRequest httpRequest) {
        return CustomerAccountResponse.from(accounts.updateAddress(sessions.required(httpRequest).getId(), request));
    }

    @GetMapping("/orders")
    public CustomerOrderPage orders(@RequestParam(defaultValue = "0") int page, HttpServletRequest request) {
        return history.list(sessions.required(request).getId(), page);
    }

    @PostMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody CustomerAccountRequests.Password request,
                                              HttpServletRequest httpRequest) {
        CustomerAccount account = accounts.changePassword(sessions.required(httpRequest).getId(), request);
        sessions.refresh(httpRequest, account);
        return Map.of("message", "Đã đổi mật khẩu.");
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        sessions.logout(request);
        return Map.of("message", "Đã đăng xuất.");
    }
}
