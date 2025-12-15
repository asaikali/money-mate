package com.example.moneymate.api.user;

import org.springframework.hateoas.RepresentationModel;

public class UserResponse extends RepresentationModel<UserResponse> {

    private final String id;
    private final String username;
    private final String provider;

    public UserResponse(String id, String username, String provider) {
        this.id = id;
        this.username = username;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getProvider() {
        return provider;
    }
}
