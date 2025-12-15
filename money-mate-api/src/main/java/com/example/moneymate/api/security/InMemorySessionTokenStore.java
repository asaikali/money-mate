package com.example.moneymate.api.security;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionTokenStore implements SessionTokenStore {

    private static final String TOKEN_PREFIX = "MMAT-";
    private final ConcurrentHashMap<String, SessionPrincipal> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<SessionPrincipal> find(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public void revoke(String token) {
        sessions.remove(token);
    }

    @Override
    public String create(String username, String obpToken) {
        String token = TOKEN_PREFIX + UUID.randomUUID();
        SessionPrincipal principal = new SessionPrincipal(username, obpToken);
        sessions.put(token, principal);
        return token;
    }
}
