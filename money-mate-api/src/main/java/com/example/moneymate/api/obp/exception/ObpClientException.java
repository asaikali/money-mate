package com.example.moneymate.api.obp.exception;

import org.springframework.http.HttpStatusCode;

public class ObpClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public ObpClientException(String message) {
        super(message);
        this.statusCode = null;
        this.responseBody = null;
    }

    public ObpClientException(String message, HttpStatusCode statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public ObpClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.responseBody = null;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
