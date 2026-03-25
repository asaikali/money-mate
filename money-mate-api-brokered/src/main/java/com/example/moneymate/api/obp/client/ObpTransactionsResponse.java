package com.example.moneymate.api.obp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ObpTransactionsResponse(
    List<Transaction> transactions
) {
    public record Transaction(
        String id,
        Details details
    ) {}

    public record Details(
        String type,
        String description,
        String posted,
        String completed,
        @JsonProperty("new_balance") Balance newBalance,
        Balance value
    ) {}

    public record Balance(
        String currency,
        String amount
    ) {}
}
