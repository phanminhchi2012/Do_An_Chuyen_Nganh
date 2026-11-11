package com.example.scalys.services;

import com.example.scalys.dtos.AdminUserRequests;
import com.example.scalys.dtos.AdminUsersResponse;
import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.models.CustomerAccount;
import com.example.scalys.models.StaffAccount;
import com.example.scalys.repositories.CustomerAccountRepository;
import com.example.scalys.repositories.CustomerPetSpendingRepository;
import com.example.scalys.repositories.StaffAccountRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
    private final StaffAccountRepository staff;
    private final CustomerAccountRepository customers;
    private final CustomerPetSpendingRepository petSpending;
    private final CustomerAccountService customerAccounts;
    private final PasswordEncoder encoder;
    private final StaffLoginLimiter limiter;
    private final String adminUsername;

    public AdminUserService(StaffAccountRepository staff, CustomerAccountRepository customers,
                            CustomerPetSpendingRepository petSpending, CustomerAccountService customerAccounts,
                            PasswordEncoder encoder, StaffLoginLimiter limiter,
                            @Value("${ADMIN_USERNAME:}") String adminUsername) {
        this.staff = staff;
        this.customers = customers;
        this.petSpending = petSpending;
        this.customerAccounts = customerAccounts;
        this.encoder = encoder;
        this.limiter = limiter;
        this.adminUsername = adminUsername;
    }

    @Transactional(readOnly = true)
    public AdminUsersResponse list() {
        var totals = petSpending.totals();
        return new AdminUsersResponse(new AdminUsersResponse.Owner(adminUsername, "ADMIN", false),
                staff.findAllByOrderByCreatedAtDescIdDesc().stream().map(AdminUsersResponse.Staff::from).toList(),
                customers.findAllByOrderByCreatedAtDescIdDesc().stream()
                        .map(account -> AdminUsersResponse.Customer.from(account,
                                totals.getOrDefault(account.getId(), BigDecimal.ZERO))).toList());
    }

    @Transactional(readOnly = true)
    public boolean isStaffEmail(String email) { return staff.existsByEmailIgnoreCase(email.trim()); }

    @Transactional
    public AdminUsersResponse.Staff createStaff(AdminUserRequests.CreateStaff request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null, null);
        validatePassword(request.password());
        StaffAccount account = staff.saveAndFlush(new StaffAccount(email, request.fullName().trim(),
                encoder.encode(request.password())));
        return AdminUsersResponse.Staff.from(account);
    }

    @Transactional
    public AdminUsersResponse.Customer createCustomer(CustomerAccountRequests.Register request) {
        CustomerAccount account = customerAccounts.register(request);
        return AdminUsersResponse.Customer.from(account, BigDecimal.ZERO);
    }

    @Transactional
    public AdminUsersResponse.Staff updateStaff(Long id, AdminUserRequests.UpdateStaff request) {
        StaffAccount account = staff.findById(id).orElseThrow(() -> notFound("nhân viên"));
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id, null);
        String passwordHash = null;
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            validatePassword(request.newPassword());
            passwordHash = encoder.encode(request.newPassword());
        }
        account.update(email, request.fullName().trim(), request.enabled(), passwordHash);
        staff.flush();
        return AdminUsersResponse.Staff.from(account);
    }

    @Transactional
    public AdminUsersResponse.Customer updateCustomer(Long id, AdminUserRequests.UpdateCustomer request) {
        CustomerAccount account = customers.findByIdForUpdate(id).orElseThrow(() -> notFound("khách hàng"));
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null, id);
        account.updateByStaff(request.fullName().trim(), email, request.phone().trim(),
                request.address().trim(), request.enabled());
        customers.flush();
        return AdminUsersResponse.Customer.from(account, petSpending.totalFor(id));
    }

    @Transactional
    public AdminUsersResponse.Customer disableCustomer(Long id) {
        CustomerAccount account = customers.findByIdForUpdate(id).orElseThrow(() -> notFound("khách hàng"));
        account.updateByStaff(account.getFullName(), account.getEmail(), account.getPhone(),
                account.getAddress(), false);
        return AdminUsersResponse.Customer.from(account, petSpending.totalFor(id));
    }

    @Transactional
    public StaffPrincipal loginStaff(String identifier, String password, String ip) {
        String email = normalizeEmail(identifier);
        String key = ip + ":" + email;
        if (limiter.locked(key)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Đăng nhập sai 5 lần. Vui lòng thử lại sau 5 phút.");
        StaffAccount account = staff.findByEmailIgnoreCase(email).orElse(null);
        if (account == null || !account.isEnabled() || !encoder.matches(password, account.getPasswordHash())) {
            limiter.failed(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không đúng.");
        }
        limiter.success(key);
        account.loggedIn();
        return StaffPrincipal.from(account);
    }

    private void ensureEmailAvailable(String email, Long selfStaffId, Long selfCustomerId) {
        if (!adminUsername.isBlank() && adminUsername.equalsIgnoreCase(email))
            throw conflict();
        var matchingStaff = staff.findByEmailIgnoreCase(email);
        if (matchingStaff.isPresent() && !matchingStaff.get().getId().equals(selfStaffId)) throw conflict();
        var matchingCustomer = customers.findByEmail(email);
        if (matchingCustomer.isPresent() && !matchingCustomer.get().getId().equals(selfCustomerId)) throw conflict();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Email không hợp lệ.");
        return value.trim().toLowerCase(Locale.ROOT);
    }
    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu nhân viên cần ít nhất 8 ký tự và không quá 72 byte.");
    }
    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email đã thuộc một tài khoản khác.");
    }
    private ResponseStatusException notFound(String kind) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản " + kind + ".");
    }
}
