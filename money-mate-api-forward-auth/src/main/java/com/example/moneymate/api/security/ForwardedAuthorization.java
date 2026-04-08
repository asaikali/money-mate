package com.example.moneymate.api.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public record ForwardedAuthorization(
    String token,
    Collection<? extends GrantedAuthority> authorities
) {

    public ForwardedAuthorization(String token) {
        this(token, Collections.emptyList());
    }
}
