package com.example.scalys.services;

import com.example.scalys.models.CustomerAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerSessionService {
    private static final String CUSTOMER_ID = "CUSTOMER_ACCOUNT_ID";
    private static final String SESSION_VERSION = "CUSTOMER_SESSION_VERSION";
    private final CustomerAccountService accounts;

    public CustomerSessionService(CustomerAccountService accounts) { this.accounts = accounts; }

    public CustomerAccount optional(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(CUSTOMER_ID) == null) return null;
        if (!(session.getAttribute(CUSTOMER_ID) instanceof Long id)
                || !(session.getAttribute(SESSION_VERSION) instanceof Integer version)) {
            clear(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hiệu lực.");
        }
        CustomerAccount account;
        try { account = accounts.get(id); }
        catch (ResponseStatusException expired) { clear(session); throw expired; }
        if (account.getSessionVersion() != version) {
            clear(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hiệu lực.");
        }
        return account;
    }

    public CustomerAccount required(HttpServletRequest request) {
        CustomerAccount account = optional(request);
        if (account == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Vui lòng đăng nhập tài khoản khách.");
        return account;
    }

    public void start(HttpServletRequest request, CustomerAccount account) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContextHolder.clearContext();
        session.setAttribute(CUSTOMER_ID, account.getId());
        session.setAttribute(SESSION_VERSION, account.getSessionVersion());
    }

    public void refresh(HttpServletRequest request, CustomerAccount account) {
        request.getSession(false).setAttribute(SESSION_VERSION, account.getSessionVersion());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) clear(session);
    }

    private void clear(HttpSession session) {
        session.removeAttribute(CUSTOMER_ID);
        session.removeAttribute(SESSION_VERSION);
    }
}
