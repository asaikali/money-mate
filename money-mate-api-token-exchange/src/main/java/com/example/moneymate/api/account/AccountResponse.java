package com.example.moneymate.api.account;

import org.springframework.hateoas.RepresentationModel;

public class AccountResponse extends RepresentationModel<AccountResponse> {

    private final String id;
    private final String accountType;
    private final String bankId;
    private final String bankName;
    private final String iban;
    private final String currency;
    private final String amount;

    public AccountResponse(String id, String accountType, String bankId, String bankName, String iban,
                          String currency, String amount) {
        this.id = id;
        this.accountType = accountType;
        this.bankId = bankId;
        this.bankName = bankName;
        this.iban = iban;
        this.currency = currency;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getIban() {
        return iban;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAmount() {
        return amount;
    }
}
