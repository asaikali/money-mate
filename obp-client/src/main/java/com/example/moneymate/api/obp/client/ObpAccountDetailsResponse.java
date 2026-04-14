package com.example.moneymate.api.obp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ObpAccountDetailsResponse(
    String id,
    String label,
    String number,
    @JsonProperty("product_code") String productCode,
    Balance balance,
    @JsonProperty("bank_id") String bankId
) {
    public record Balance(
        String currency,
        String amount
    ) {}
}
