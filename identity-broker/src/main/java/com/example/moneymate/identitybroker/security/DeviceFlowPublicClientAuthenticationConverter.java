package com.example.moneymate.identitybroker.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Enables public-client authentication for OAuth device flow endpoints.
 */
public final class DeviceFlowPublicClientAuthenticationConverter implements AuthenticationConverter {

    private static final String DEVICE_CODE_GRANT_TYPE =
        "urn:ietf:params:oauth:grant-type:device_code";
    static final String DEVICE_FLOW_MARKER_KEY = "device_flow_public_client";

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        String requestUri = request.getRequestURI();
        boolean isDeviceAuthorizationRequest = requestUri.endsWith("/oauth2/device_authorization");
        boolean isDeviceTokenRequest = requestUri.endsWith("/oauth2/token")
            && DEVICE_CODE_GRANT_TYPE.equals(request.getParameter("grant_type"));

        if (!isDeviceAuthorizationRequest && !isDeviceTokenRequest) {
            return null;
        }

        String[] clientIds = request.getParameterValues("client_id");
        if (clientIds == null || clientIds.length != 1 || !StringUtils.hasText(clientIds[0])) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (!"client_id".equals(key) && values.length > 0) {
                additionalParameters.put(key, values[0]);
            }
        });
        additionalParameters.put(DEVICE_FLOW_MARKER_KEY, "true");

        return new OAuth2ClientAuthenticationToken(
            clientIds[0],
            ClientAuthenticationMethod.NONE,
            null,
            additionalParameters
        );
    }
}
