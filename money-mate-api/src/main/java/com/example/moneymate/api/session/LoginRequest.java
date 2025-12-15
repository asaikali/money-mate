package com.example.moneymate.api.session;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank
    String username,

    @NotBlank
    String password
) {
}
