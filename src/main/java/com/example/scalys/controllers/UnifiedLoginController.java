package com.example.scalys.controllers;

import com.example.scalys.dtos.CustomerAccountRequests;
import com.example.scalys.dtos.UnifiedLoginRequest;
import com.example.scalys.dtos.UnifiedLoginResponse;
import com.example.scalys.services.CustomerAccountService;
import com.example.scalys.services.CustomerSessionService;
import com.example.scalys.services.AdminUserService;
import com.example.scalys.services.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class UnifiedLoginController {
    private static final String BAD_LOGIN = "Tài khoản hoặc mật khẩu không đúng.";
    private static final String LOCKED = "Đăng nhập sai 5 lần. Vui lòng thử lại sau 5 phút.";
    private final String adminUsername;
    private final UserDetailsService adminUsers;
    private final PasswordEncoder encoder;
    private final LoginAttemptService adminAttempts;
    private final CustomerAccountService accounts;
    private final CustomerSessionService sessions;
    private final AdminUserService staff;
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();

    public UnifiedLoginController(@Value("${ADMIN_USERNAME:}") String adminUsername,
                                  UserDetailsService adminUsers, PasswordEncoder encoder,
                                  LoginAttemptService adminAttempts, CustomerAccountService accounts,
                                  CustomerSessionService sessions, AdminUserService staff) {
        this.adminUsername = adminUsername;
        this.adminUsers = adminUsers;
        this.encoder = encoder;
        this.adminAttempts = adminAttempts;
        this.accounts = accounts;
        this.sessions = sessions;
        this.staff = staff;
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public UnifiedLoginResponse login(@Valid @RequestBody UnifiedLoginRequest input,
                                      HttpServletRequest request, HttpServletResponse response) {
        String identifier = input.identifier().trim();
        if (!adminUsername.isBlank() && adminUsername.equals(identifier)) {
            if (adminAttempts.isLocked()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, LOCKED);
            var admin = adminUsers.loadUserByUsername(identifier);
            if (!encoder.matches(input.password(), admin.getPassword())) {
                boolean locked = adminAttempts.recordFailure();
                throw new ResponseStatusException(locked ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED,
                        locked ? LOCKED : BAD_LOGIN);
            }
            adminAttempts.reset();
            request.getSession(true);
            request.changeSessionId();
            sessions.logout(request);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    admin, null, admin.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContexts.saveContext(context, request, response);
            return new UnifiedLoginResponse("ADMIN");
        }
        if (!identifier.contains("@")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_LOGIN);
        if (staffEmail(identifier)) {
            var principal = staff.loginStaff(identifier, input.password(), request.getRemoteAddr());
            request.getSession(true);
            request.changeSessionId();
            sessions.logout(request);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContexts.saveContext(context, request, response);
            return new UnifiedLoginResponse("STAFF");
        }
        var account = accounts.login(new CustomerAccountRequests.Login(identifier, input.password()),
                request.getRemoteAddr());
        sessions.start(request, account);
        return new UnifiedLoginResponse("CUSTOMER");
    }

    private boolean staffEmail(String identifier) {
        return staff.isStaffEmail(identifier);
    }
}
