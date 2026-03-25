package com.example.moneymate.api.transaction;

import org.springframework.hateoas.RepresentationModel;

public class TransactionResponse extends RepresentationModel<TransactionResponse> {

    private final String date;
    private final String description;
    private final String amount;
    private final String currency;
    private final String balanceAfter;

    public TransactionResponse(String date, String description, String amount, String currency, String balanceAfter) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBalanceAfter() {
        return balanceAfter;
    }
}
