package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ForwardedObpClient extends ObpClient {

    private static final Logger log = LoggerFactory.getLogger(ForwardedObpClient.class);

    private final String baseUrl;

    public ForwardedObpClient(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties
    ) {
        super(publicRestClient, properties);
        this.baseUrl = properties.api().baseUrl();
        log.info("Configured OBP public RestClient baseUrl={} apiVersion={}", baseUrl, apiVersion);
    }

    @Override
    protected void applyAuth(RestClient.RequestHeadersSpec<?> spec, String token) {
        logOutgoingRequest(token);
        spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private void logOutgoingRequest(String bearerToken) {
        log.info("Outgoing RestClient request authorizationHeader=Bearer {}",
            previewToken(bearerToken));
    }

    private String previewToken(String token) {
        if (token == null || token.isBlank()) {
            return "<empty>";
        }
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
