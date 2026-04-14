package com.example.moneymate.api.obp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ObpAccountsResponse(
    List<Account> accounts
) {
    public record Account(
        String id,
        String label,
        @JsonProperty("bank_id") String bankId,
        @JsonProperty("account_type") String accountType,
        @JsonProperty("account_routings") List<AccountRouting> accountRoutings
    ) {}

    public record AccountRouting(
        String scheme,
        String address
    ) {}
}
