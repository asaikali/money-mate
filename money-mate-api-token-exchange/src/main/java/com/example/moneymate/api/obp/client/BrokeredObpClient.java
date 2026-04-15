package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class BrokeredObpClient extends ObpClient {

    private static final Logger log = LoggerFactory.getLogger(BrokeredObpClient.class);

    private final String consumerKey;

    public BrokeredObpClient(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties
    ) {
        super(publicRestClient, properties);
        this.consumerKey = properties.auth().consumerKey();
    }

    @Override
    protected void applyAuth(RestClient.RequestHeadersSpec<?> spec, String token) {
        spec.header("directlogin", "token=" + token);
    }

    public String login(String username, String password) {
        String directLoginHeader = String.format(
            "username=%s, password=%s, consumer_key=%s",
            username, password, consumerKey
        );

        log.debug("Attempting OBP DirectLogin for user: {}", username);

        try {
            DirectLoginResponse response = publicRestClient.post()
                .uri("/my/logins/direct")
                .header("directlogin", directLoginHeader)
                .body("{}")
                .retrieve()
                .body(DirectLoginResponse.class);

            if (response == null || response.token() == null) {
                log.error("OBP DirectLogin returned null response or token for user: {}", username);
                throw new ObpAuthenticationException("OBP authentication failed: no token received");
            }

            log.info("Successfully authenticated user: {}", username);
            return response.token();

        } catch (RestClientException e) {
            log.error("OBP DirectLogin failed for user {}: {}", username, e.getMessage(), e);
            throw new ObpAuthenticationException("OBP authentication failed", e);
        }
    }
}
