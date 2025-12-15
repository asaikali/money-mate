package com.example.moneymate.api.security;

import java.util.Optional;

public interface SessionTokenStore {
    /**
     * Find a session by token
     * @param token the bearer token
     * @return the session principal if found
     */
    Optional<SessionPrincipal> find(String token);

    /**
     * Revoke (invalidate) a token
     * @param token the bearer token to revoke
     */
    void revoke(String token);

    /**
     * Create a new session and return the generated token
     * @param username the username for the session
     * @return the generated MMAT token
     */
    String create(String username);
}
