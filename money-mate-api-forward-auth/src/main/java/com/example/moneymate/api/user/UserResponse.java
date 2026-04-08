package com.example.moneymate.api.user;

import org.springframework.hateoas.RepresentationModel;

public class UserResponse extends RepresentationModel<UserResponse> {

    private final String username;
    private final String email;
    private final int accountCount;
    private final int bankCount;

    public UserResponse(String username, String email, int accountCount, int bankCount) {
        this.username = username;
        this.email = email;
        this.accountCount = accountCount;
        this.bankCount = bankCount;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public int getBankCount() {
        return bankCount;
    }
}
