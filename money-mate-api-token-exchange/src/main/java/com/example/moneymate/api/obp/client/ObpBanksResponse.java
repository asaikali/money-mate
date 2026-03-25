package com.example.moneymate.api.obp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ObpBanksResponse(
    List<Bank> banks
) {
    public record Bank(
        String id,
        @JsonProperty("short_name") String shortName,
        @JsonProperty("full_name") String fullName,
        String logo,
        String website
    ) {}
}
