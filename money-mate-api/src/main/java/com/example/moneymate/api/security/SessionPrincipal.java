package com.example.moneymate.api.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public record SessionPrincipal(
    String subject,
    Collection<? extends GrantedAuthority> authorities
) {
    public SessionPrincipal(String subject) {
        this(subject, Collections.emptyList());
    }

    public Object principal() {
        return subject;
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return authorities;
    }
}
