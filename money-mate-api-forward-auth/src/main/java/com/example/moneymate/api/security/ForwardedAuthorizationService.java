package com.example.moneymate.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ForwardedAuthorizationService {

    public String currentBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ForwardedAuthorization forwardedAuthorization)) {
            throw new IllegalStateException("Current request is not authenticated with a forwarded bearer token");
        }
        return forwardedAuthorization.token();
    }
}
