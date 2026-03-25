package com.example.moneymate.identitybroker.security;

import com.example.moneymate.identitybroker.config.IdentityBrokerProperties;
import com.example.moneymate.identitybroker.obp.ObpDirectLoginService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Map;

public class ObpTokenExchangeAuthenticationProvider implements AuthenticationProvider {

    static final String TOKEN_EXCHANGE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    static final AuthorizationGrantType TOKEN_EXCHANGE_GRANT = new AuthorizationGrantType(TOKEN_EXCHANGE_GRANT_TYPE);
    static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";
    static final String OBP_RESOURCE = "obp-sandbox";

    private final JwtDecoder jwtDecoder;
    private final IdentityBrokerProperties identityBrokerProperties;
    private final ObpDirectLoginService obpDirectLoginService;

    public ObpTokenExchangeAuthenticationProvider(
        JwtDecoder jwtDecoder,
        IdentityBrokerProperties identityBrokerProperties,
        ObpDirectLoginService obpDirectLoginService
    ) {
        this.jwtDecoder = jwtDecoder;
        this.identityBrokerProperties = identityBrokerProperties;
        this.obpDirectLoginService = obpDirectLoginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws OAuth2AuthenticationException {
        OAuth2TokenExchangeAuthenticationToken tokenExchange =
            (OAuth2TokenExchangeAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = authenticatedClient(tokenExchange);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null || registeredClient.getAuthorizationGrantTypes().stream()
            .noneMatch(authorizationGrantType -> TOKEN_EXCHANGE_GRANT_TYPE.equals(authorizationGrantType.getValue()))) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                "The client is not allowed to use token exchange"
            );
        }

        if (!ACCESS_TOKEN_TYPE.equals(tokenExchange.getSubjectTokenType())) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.INVALID_REQUEST,
                "Only access_token subject_token_type is supported"
            );
        }

        if (!tokenExchange.getResources().isEmpty() && !tokenExchange.getResources().contains(OBP_RESOURCE)) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.INVALID_REQUEST,
                "Unsupported resource requested"
            );
        }

        Jwt subjectToken = decodeSubjectToken(tokenExchange.getSubjectToken());
        String username = subjectToken.getSubject();
        if (username == null || username.isBlank()) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.INVALID_GRANT,
                "The subject token does not contain a subject"
            );
        }

        IdentityBrokerProperties.DemoUser demoUser;
        try {
            demoUser = identityBrokerProperties.findDemoUser(username);
        } catch (IllegalStateException exception) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.INVALID_GRANT,
                "No OBP sandbox user is configured for the authenticated subject",
                exception
            );
        }
        String obpToken = obpDirectLoginService.login(demoUser.getUsername(), demoUser.getPassword());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            obpToken,
            issuedAt,
            expiresAt
        );

        return new OAuth2AccessTokenAuthenticationToken(
            registeredClient,
            clientPrincipal,
            accessToken,
            null,
            Map.of("issued_token_type", ACCESS_TOKEN_TYPE)
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2TokenExchangeAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2ClientAuthenticationToken authenticatedClient(OAuth2TokenExchangeAuthenticationToken authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2ClientAuthenticationToken clientAuthentication && clientAuthentication.isAuthenticated()) {
            return clientAuthentication;
        }
        throw new TokenExchangeAuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication is required");
    }

    private Jwt decodeSubjectToken(String subjectToken) {
        try {
            return jwtDecoder.decode(subjectToken);
        } catch (JwtException exception) {
            throw new TokenExchangeAuthenticationException(
                OAuth2ErrorCodes.INVALID_GRANT,
                "The subject token is invalid",
                exception
            );
        }
    }
}
