package com.example.moneymate.identitybroker.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;

/**
 * Authenticates public clients (client_authentication_method=none) for device flow endpoints.
 */
public final class DeviceFlowPublicClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    public DeviceFlowPublicClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication)) {
            return null;
        }

        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            return null;
        }

        Object marker = clientAuthentication.getAdditionalParameters()
            .get(DeviceFlowPublicClientAuthenticationConverter.DEVICE_FLOW_MARKER_KEY);
        if (!"true".equals(String.valueOf(marker))) {
            return null;
        }

        String clientId = String.valueOf(clientAuthentication.getPrincipal());
        if (!StringUtils.hasText(clientId)) {
            throw invalidClient(OAuth2ParameterNames.CLIENT_ID);
        }

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw invalidClient(OAuth2ParameterNames.CLIENT_ID);
        }

        if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw invalidClient("authentication_method");
        }

        return new OAuth2ClientAuthenticationToken(
            registeredClient,
            ClientAuthenticationMethod.NONE,
            null
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2AuthenticationException invalidClient(String parameterName) {
        return new OAuth2AuthenticationException(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT),
            parameterName
        );
    }
}
