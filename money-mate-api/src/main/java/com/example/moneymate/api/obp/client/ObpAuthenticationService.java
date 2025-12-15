package com.example.moneymate.api.obp.client;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ObpAuthenticationService {

    private final RestClient publicRestClient;
    private final ObpProperties properties;
    private volatile String cachedToken;

    public ObpAuthenticationService(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties
    ) {
        this.publicRestClient = publicRestClient;
        this.properties = properties;
    }

    public String authenticate() {
        if (cachedToken != null) {
            return cachedToken;
        }

        DirectLoginResponse response = publicRestClient.post()
            .uri("/my/logins/direct")
            .header("directlogin", buildDirectLoginHeader())
            .body("{}")
            .retrieve()
            .body(DirectLoginResponse.class);

        if (response != null && response.token() != null) {
            this.cachedToken = response.token();
            return cachedToken;
        }

        throw new IllegalStateException("Failed to authenticate: no token received");
    }

    private String buildDirectLoginHeader() {
        return String.format(
            "username=%s, password=%s, consumer_key=%s",
            properties.auth().username(),
            properties.auth().password(),
            properties.auth().consumerKey()
        );
    }

    public void clearToken() {
        this.cachedToken = null;
    }
}
