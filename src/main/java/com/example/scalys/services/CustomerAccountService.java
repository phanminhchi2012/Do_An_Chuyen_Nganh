package com.example.scalys.services;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.models.CustomerAccount;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerAccountService {
    private final CustomerAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final CustomerLoginLimiter limiter;
    private final String adminUsername;
    private final PromotionService promotions;
    private final StaffAccountRepository staff;

    public CustomerAccountService(CustomerAccountRepository accounts, PasswordEncoder encoder,
                                  CustomerLoginLimiter limiter,
                                  @Value("${ADMIN_USERNAME:}") String adminUsername,
                                  PromotionService promotions, StaffAccountRepository staff) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.limiter = limiter;
        this.adminUsername = adminUsername;
        this.promotions = promotions;
        this.staff = staff;
    }

    @Transactional
    public CustomerAccount register(CustomerAccountRequests.Register request) {
        String email = normalizeEmail(request.email());
        if (!adminUsername.isBlank() && adminUsername.equalsIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này không thể dùng để đăng ký tài khoản khách.");
        if (staff.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã thuộc tài khoản nhân viên.");
        String name = requiredName(request.fullName());
        String phone = normalizePhone(request.phone());
        validatePassword(request.password());
        if (accounts.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký.");
        try {
            CustomerAccount account = accounts.saveAndFlush(new CustomerAccount(email, encoder.encode(request.password()),
                    name, phone, requiredAddress(request.address())));
            promotions.issueForAccount(account);
            return account;
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký.");
        }
    }

    public CustomerAccount login(CustomerAccountRequests.Login request, String remoteAddress) {
        String email = normalizeEmail(request.email());
        String key = remoteAddress + ":" + email;
        if (limiter.locked(key)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Bạn đã nhập sai nhiều lần. Vui lòng thử lại sau 5 phút.");
        CustomerAccount account = accounts.findByEmail(email).orElse(null);
        if (account == null || !account.isEnabled()
                || account.getPasswordHash() == null
                || !encoder.matches(request.password(), account.getPasswordHash())) {
            limiter.failed(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        }
        limiter.success(key);
        return account;
    }

    public CustomerAccount get(Long id) {
        CustomerAccount account = accounts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không còn hợp lệ."));
        if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Tài khoản đã bị vô hiệu hóa.");
        return account;
    }

    @Transactional
    public CustomerAccount updateAddress(Long id, CustomerAccountRequests.Address request) {
        CustomerAccount account = get(id);
        account.updateAddress(requiredAddress(request.address()));
        return account;
    }

    @Transactional
    public CustomerAccount changePassword(Long id, CustomerAccountRequests.Password request) {
        CustomerAccount account = get(id);
        if (account.getPasswordHash() == null
                || !encoder.matches(request.currentPassword(), account.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng.");
        validatePassword(request.newPassword());
        account.changePassword(encoder.encode(request.newPassword()));
        return account;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 254 || !email.contains("@"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String requiredName(String name) {
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập họ tên.");
        return name.trim();
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[\\s.-]", "");
    }

    private static String requiredAddress(String address) {
        if (address == null || address.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập địa chỉ.");
        return address.trim();
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu cần ít nhất 8 ký tự và không quá 72 byte.");
    }
}
