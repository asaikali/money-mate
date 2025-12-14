package com.example.moneymate.api.obp.exception;

import org.springframework.http.HttpStatusCode;

public class ObpAuthenticationException extends ObpClientException {

    public ObpAuthenticationException(String message) {
        super(message);
    }

    public ObpAuthenticationException(String message, HttpStatusCode statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }

    public ObpAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
