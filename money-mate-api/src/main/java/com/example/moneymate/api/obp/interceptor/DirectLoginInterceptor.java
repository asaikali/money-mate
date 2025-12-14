package com.example.moneymate.api.obp.interceptor;

import com.example.moneymate.api.obp.client.ObpAuthenticationService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class DirectLoginInterceptor implements ClientHttpRequestInterceptor {

    private final ObpAuthenticationService authService;

    public DirectLoginInterceptor(ObpAuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {
        String token = authService.authenticate();

        request.getHeaders().set("directlogin", "token=" + token);

        return execution.execute(request, body);
    }
}
