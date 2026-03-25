package com.example.moneymate.api.obp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDetailsResponse(
    @JsonProperty("user_id") String userId,
    String email,
    String username,
    @JsonProperty("provider_id") String providerId,
    String provider
) {
}
