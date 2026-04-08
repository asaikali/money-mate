package com.example.moneymate.envoyegressgateway.auth;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class IdentityBrokerTokenExchangeService {

    private static final String TOKEN_EXCHANGE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

    private final RestClient restClient;
    private final IdentityBrokerProperties identityBrokerProperties;

    public IdentityBrokerTokenExchangeService(RestClient.Builder restClientBuilder,
                                              IdentityBrokerProperties identityBrokerProperties) {
        this.restClient = restClientBuilder.build();
        this.identityBrokerProperties = identityBrokerProperties;
    }

    public String exchangeForObpToken(String subjectToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", TOKEN_EXCHANGE_GRANT_TYPE);
        formData.add("subject_token", subjectToken);
        formData.add("subject_token_type", ACCESS_TOKEN_TYPE);
        formData.add("requested_token_type", ACCESS_TOKEN_TYPE);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri(identityBrokerProperties.getTokenUri())
                .headers(headers -> headers.setBasicAuth(
                    identityBrokerProperties.getTokenExchangeClientId(),
                    identityBrokerProperties.getTokenExchangeClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

            if (response == null || !(response.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
                throw new IllegalStateException("Identity broker token exchange did not return an access_token");
            }

            return accessToken;
        } catch (RestClientException exception) {
            throw new IllegalStateException("Identity broker token exchange failed", exception);
        }
    }
}
