package com.example.scalys.configs;

import com.example.scalys.services.LoginAttemptService;
import com.example.scalys.services.CustomerSessionService;
import com.example.scalys.services.StaffPrincipal;
import com.example.scalys.services.AccountAuditService;
import com.example.scalys.services.CustomerGoogleLoginService;
import com.example.scalys.repositories.StaffAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {
    private static final String LOGIN_PATH = "/api/admin/auth/login";
    private static final String LOCK_MESSAGE = "Đăng nhập sai 5 lần. Vui lòng thử lại sau 5 phút.";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUser(
            @Value("${ADMIN_USERNAME:}") String username,
            @Value("${ADMIN_PASSWORD_HASH:}") String passwordHash,
            Environment environment) {
        if (username.isBlank() != passwordHash.isBlank()
                || (environment.acceptsProfiles(Profiles.of("prod")) && username.isBlank())) {
            throw new IllegalArgumentException("Cần đặt ADMIN_USERNAME và ADMIN_PASSWORD_HASH trong biến môi trường.");
        }
        var users = new InMemoryUserDetailsManager();
        if (!username.isBlank() && !passwordHash.isBlank()) {
            if (!passwordHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
                throw new IllegalArgumentException("ADMIN_PASSWORD_HASH phải là mã BCrypt hợp lệ.");
            }
            users.createUser(User.withUsername(username).password(passwordHash).roles("ADMIN").build());
        }
        return users;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAttemptService attempts,
                                            CustomerSessionService customerSessions,
                                            StaffAccountRepository staffAccounts,
                                            AccountAuditService audit,
                                            CustomerGoogleLoginService google,
                                            ObjectProvider<ClientRegistrationRepository> registrations,
                                            @Value("${spring.security.oauth2.client.registration.google.redirect-uri:}")
                                            String googleRedirectUri) throws Exception {
        var csrfRepository = new HttpSessionCsrfTokenRepository();
        http
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                .ignoringRequestMatchers("/api/orders", "/api/orders/cart", "/api/orders/cart/vnpay",
                        "/api/orders/zalopay", "/api/orders/cart/zalopay",
                        "/api/orders/lookup", "/api/orders/close-hold", "/api/orders/cancel-hold"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/products", "/api/products/*", "/api/product-images/*",
                        "/api/admin/auth/csrf", "/api/account/csrf",
                        "/api/account/google/availability",
                        "/api/payments/vnpay/availability", "/api/payments/vnpay/orders/*",
                        "/api/payments/vnpay/verify-return", "/api/payments/vnpay/ipn",
                        "/api/payments/zalopay/availability", "/api/payments/zalopay/orders/*",
                        "/api/payments/zalopay/verify-return").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/contact", "/api/warranty-claims", "/api/auth/login", LOGIN_PATH,
                        "/api/orders", "/api/orders/cart", "/api/orders/cart/vnpay",
                        "/api/orders/zalopay", "/api/orders/cart/zalopay",
                        "/api/orders/lookup", "/api/orders/close-hold", "/api/orders/cancel-hold",
                        "/api/promotions/quote", "/api/account/register", "/api/account/login",
                        "/api/account/password", "/api/account/logout").permitAll()
                // Customer authentication is stored in a separate session; these controllers call required().
                .requestMatchers(HttpMethod.GET,
                        "/api/account/me", "/api/account/orders", "/api/account/promotions").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/account/me").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/audit", "/api/admin/promotions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/users/staff", "/api/admin/promotions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/users/staff/*", "/api/admin/promotions/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/admin/auth/session", "/api/admin/dashboard", "/api/admin/products",
                        "/api/admin/orders", "/api/admin/orders/pending-cod-count",
                        "/api/admin/contacts", "/api/admin/contacts/new-count",
                        "/api/admin/warranty-claims", "/api/admin/warranty-claims/new-count",
                        "/api/admin/warranty-claims/*/video", "/api/admin/users")
                    .hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.POST, "/api/admin/auth/logout", "/api/admin/products",
                        "/api/admin/product-images", "/api/admin/users/customers")
                    .hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/admin/products/*", "/api/admin/users/customers/*")
                    .hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PATCH,
                        "/api/admin/products/*/stock", "/api/admin/products/*/status",
                        "/api/admin/orders/*/status", "/api/admin/orders/*/confirm-balance",
                        "/api/admin/orders/*/not-received", "/api/admin/contacts/*/status",
                        "/api/admin/warranty-claims/*/status").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/users/customers/*")
                    .hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/**").denyAll()
                .anyRequest().permitAll())
            .formLogin(form -> form.loginProcessingUrl(LOGIN_PATH)
                .successHandler((request, response, authentication) -> {
                    attempts.reset();
                    customerSessions.logout(request);
                    audit.record("ADMIN", authentication.getName(), "LOGIN", "SESSION", null,
                            request.getServletPath(), request.getRemoteAddr());
                    writeJson(response, 200, "{\"username\":\"" + escapeJson(authentication.getName()) + "\"}");
                })
                .failureHandler((request, response, exception) -> {
                    boolean locked = attempts.recordFailure();
                    writeMessage(response, locked ? 429 : 401,
                        locked ? LOCK_MESSAGE : "Tên đăng nhập hoặc mật khẩu không đúng.");
                }))
            .logout(logout -> logout.logoutUrl("/api/admin/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        String role = authentication.getAuthorities().stream().anyMatch(item ->
                                "ROLE_ADMIN".equals(item.getAuthority())) ? "ADMIN" : "STAFF";
                        audit.record(role, authentication.getName(), "LOGOUT", "SESSION", null,
                                request.getServletPath(), request.getRemoteAddr());
                    }
                    writeMessage(response, 200, "Đã đăng xuất.");
                }))
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                writeMessage(response, 401, "Vui lòng đăng nhập để tiếp tục.")))
            .addFilterBefore(new AdminAuthenticationFilter(staffAccounts), CsrfFilter.class)
            .addFilterBefore(new LoginLockFilter(attempts), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AccountAuditFilter(audit, customerSessions), UsernamePasswordAuthenticationFilter.class);
        ClientRegistrationRepository googleRegistration = registrations.getIfAvailable();
        if (googleRegistration != null) {
            String frontendOrigin = googleFrontendOrigin(googleRedirectUri);
            var handler = new GoogleLoginRedirectHandler(google, frontendOrigin);
            var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                    googleRegistration, "/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(
                    parameters -> parameters.put("prompt", "select_account")));
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                    .successHandler(handler).failureHandler(handler));
        }
        return http.build();
    }

    static String googleFrontendOrigin(String redirectUri) {
        URI uri = URI.create(redirectUri);
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && ("127.0.0.1".equalsIgnoreCase(uri.getHost()) || "localhost".equalsIgnoreCase(uri.getHost()));
        if ((!"https".equalsIgnoreCase(uri.getScheme()) && !localHttp)
                || uri.getHost() == null || uri.getRawAuthority() == null
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !"/login/oauth2/code/google".equals(uri.getPath()))
            throw new IllegalArgumentException("Google redirect URI phải là callback HTTPS hoặc loopback hợp lệ.");
        return uri.getScheme() + "://" + uri.getRawAuthority();
    }

    private static void writeMessage(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, "{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private static void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class AdminAuthenticationFilter extends OncePerRequestFilter {
        private final StaffAccountRepository staffAccounts;
        AdminAuthenticationFilter(StaffAccountRepository staffAccounts) {
            this.staffAccounts = staffAccounts;
        }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            String path = request.getServletPath();
            if (path.startsWith("/api/admin/")
                    && !path.equals(LOGIN_PATH)
                    && !path.equals("/api/admin/auth/csrf")) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()
                        || authentication instanceof AnonymousAuthenticationToken) {
                    writeMessage(response, 401, "Vui lòng đăng nhập để tiếp tục.");
                    return;
                }
                if (authentication.getPrincipal() instanceof StaffPrincipal principal) {
                    var staff = staffAccounts.findById(principal.id()).orElse(null);
                    if (staff == null || !staff.isEnabled()
                            || staff.getSessionVersion() != principal.sessionVersion()) {
                        SecurityContextHolder.clearContext();
                        writeMessage(response, 401, "Phiên nhân viên đã hết hiệu lực.");
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        }
    }

    private static class LoginLockFilter extends OncePerRequestFilter {
        private final LoginAttemptService attempts;

        LoginLockFilter(LoginAttemptService attempts) {
            this.attempts = attempts;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            if (LOGIN_PATH.equals(request.getServletPath()) && "POST".equals(request.getMethod())
                    && attempts.isLocked()) {
                writeMessage(response, 429, LOCK_MESSAGE);
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
