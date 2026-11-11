package com.example.scalys.services;

import com.example.scalys.models.CustomerAccount;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerGoogleLoginService {
    private final CustomerAccountRepository accounts;
    private final CustomerSessionService sessions;
    private final StaffAccountRepository staff;
    private final PromotionService promotions;
    private final String adminUsername;

    public CustomerGoogleLoginService(CustomerAccountRepository accounts,
                                      CustomerSessionService sessions, StaffAccountRepository staff,
                                      PromotionService promotions, @Value("${ADMIN_USERNAME:}") String adminUsername) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.staff = staff;
        this.promotions = promotions;
        this.adminUsername = adminUsername;
    }

    @Transactional
    public void begin(HttpServletRequest request, String subject, String email, String fullName) {
        if (subject == null || subject.isBlank() || email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google không trả về danh tính hợp lệ.");
        CustomerAccount linked = accounts.findByGoogleSubject(subject).orElse(null);
        if (linked != null) {
            if (!linked.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị vô hiệu hóa.");
            sessions.start(request, linked);
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if ((!adminUsername.isBlank() && adminUsername.equalsIgnoreCase(normalized))
                || staff.existsByEmailIgnoreCase(normalized))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản quản trị và nhân viên không dùng Google.");
        CustomerAccount existing = accounts.findByEmail(normalized).orElse(null);
        if (existing != null) {
            if (!existing.isEnabled() || existing.getGoogleSubject() != null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không thể liên kết với Google.");
            existing.linkGoogle(subject);
            accounts.saveAndFlush(existing);
            sessions.start(request, existing);
            return;
        }
        try {
            CustomerAccount account = accounts.saveAndFlush(CustomerAccount.fromGoogle(
                    normalized, subject, googleName(fullName, normalized), null, null));
            promotions.issueForAccount(account);
            sessions.start(request, account);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản vừa được tạo. Vui lòng thử đăng nhập lại.");
        }
    }

    private static String googleName(String fullName, String email) {
        String name = fullName == null ? "" : fullName.trim();
        if (name.isBlank()) name = email.substring(0, email.indexOf('@'));
        return name.length() > 150 ? name.substring(0, 150) : name;
    }
}
