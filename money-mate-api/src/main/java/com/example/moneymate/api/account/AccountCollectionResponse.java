package com.example.moneymate.api.account;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

public class AccountCollectionResponse extends RepresentationModel<AccountCollectionResponse> {

    private final int accountCount;
    private final List<AccountResponse> accounts;

    public AccountCollectionResponse(int accountCount, List<AccountResponse> accounts) {
        this.accountCount = accountCount;
        this.accounts = accounts;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public List<AccountResponse> getAccounts() {
        return accounts;
    }
}
