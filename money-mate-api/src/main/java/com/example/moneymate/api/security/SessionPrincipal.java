package com.example.moneymate.api.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public record SessionPrincipal(
    String subject,
    String obpToken,
    Collection<? extends GrantedAuthority> authorities
) {
    public SessionPrincipal(String subject, String obpToken) {
        this(subject, obpToken, Collections.emptyList());
    }

    public Object principal() {
        return subject;
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return authorities;
    }
}
