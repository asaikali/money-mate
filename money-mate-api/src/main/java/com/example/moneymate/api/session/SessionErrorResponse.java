package com.example.moneymate.api.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionErrorResponse(
    String error,
    String message,

    @JsonProperty("upstream_status")
    Integer upstreamStatus
) {
    public static SessionErrorResponse authenticationFailed(String message, Integer upstreamStatus) {
        return new SessionErrorResponse("authentication_failed", message, upstreamStatus);
    }
}
