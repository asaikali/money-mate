package com.example.moneymate.identitybroker.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class TokenExchangeAuthenticationException extends OAuth2AuthenticationException {

    public TokenExchangeAuthenticationException(String errorCode, String description) {
        super(new OAuth2Error(errorCode, description, null));
    }

    public TokenExchangeAuthenticationException(String errorCode, String description, Throwable cause) {
        super(new OAuth2Error(errorCode, description, null), cause);
    }
}
