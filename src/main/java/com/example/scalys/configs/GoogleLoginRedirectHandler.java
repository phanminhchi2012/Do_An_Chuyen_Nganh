package com.example.scalys.configs;

import com.example.scalys.services.CustomerGoogleLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

final class GoogleLoginRedirectHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final CustomerGoogleLoginService google;
    private final String frontendOrigin;

    GoogleLoginRedirectHandler(CustomerGoogleLoginService google, String frontendOrigin) {
        this.google = google;
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser user)
                || !Boolean.TRUE.equals(user.getEmailVerified())) {
            response.sendRedirect(frontendOrigin + "/?auth=login&googleError=invalid");
            return;
        }
        try {
            google.begin(request, user.getSubject(), user.getEmail(), user.getFullName());
            response.sendRedirect(frontendOrigin + "/?googleLogin=success");
        } catch (RuntimeException error) {
            response.sendRedirect(frontendOrigin + "/?auth=login&googleError=unavailable");
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException error) throws IOException, ServletException {
        response.sendRedirect(frontendOrigin + "/?auth=login&googleError=failed");
    }
}
