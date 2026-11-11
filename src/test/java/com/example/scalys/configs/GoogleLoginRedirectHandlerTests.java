package com.example.scalys.configs;

import com.example.scalys.services.CustomerGoogleLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleLoginRedirectHandlerTests {
    @Test
    void redirectsGoogleCustomersToHomePage() throws Exception {
        var google = mock(CustomerGoogleLoginService.class);
        var user = mock(OidcUser.class);
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(user.getEmailVerified()).thenReturn(true);
        when(user.getSubject()).thenReturn("google-subject");
        when(user.getEmail()).thenReturn("customer@example.test");
        when(user.getFullName()).thenReturn("Customer");
        var handler = new GoogleLoginRedirectHandler(google, "http://127.0.0.1:2003");

        var first = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), first, authentication);
        assertEquals("http://127.0.0.1:2003/?googleLogin=success", first.getRedirectedUrl());

        var returning = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), returning, authentication);
        assertEquals("http://127.0.0.1:2003/?googleLogin=success", returning.getRedirectedUrl());
    }

    @Test
    void errorsAlsoReturnToFrontend() throws Exception {
        var handler = new GoogleLoginRedirectHandler(mock(CustomerGoogleLoginService.class),
                "http://127.0.0.1:2003");
        var response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("test"));
        assertEquals("http://127.0.0.1:2003/?auth=login&googleError=failed", response.getRedirectedUrl());
    }

    @Test
    void frontendOriginComesFromRegisteredGoogleCallback() {
        assertEquals("http://127.0.0.1:2003", SecurityConfig.googleFrontendOrigin(
                "http://127.0.0.1:2003/login/oauth2/code/google"));
        assertThrows(IllegalArgumentException.class, () -> SecurityConfig.googleFrontendOrigin(
                "http://example.com/login/oauth2/code/google"));
    }
}
