package com.example.moneymate.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForwardedAuthorizationServiceTest {

    private final ForwardedAuthorizationService service = new ForwardedAuthorizationService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentBearerTokenReturnsForwardedToken() {
        ForwardedAuthorization principal = new ForwardedAuthorization("forwarded-token");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, principal.token(), principal.authorities())
        );

        assertThat(service.currentBearerToken()).isEqualTo("forwarded-token");
    }

    @Test
    void currentBearerTokenFailsWithoutForwardedPrincipal() {
        assertThatThrownBy(service::currentBearerToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("forwarded bearer token");
    }
}
