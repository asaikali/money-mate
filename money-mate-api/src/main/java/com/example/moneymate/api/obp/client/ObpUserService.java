package com.example.moneymate.api.obp.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ObpUserService {

    private final RestClient authenticatedRestClient;
    private final ObpProperties properties;

    public ObpUserService(
        @Qualifier("obpAuthenticatedRestClient") RestClient authenticatedRestClient,
        ObpProperties properties
    ) {
        this.authenticatedRestClient = authenticatedRestClient;
        this.properties = properties;
    }

    public UserDetailsResponse getCurrentUser() {
        String uri = "/obp/" + properties.api().version() + "/users/current";

        return authenticatedRestClient.get()
            .uri(uri)
            .retrieve()
            .body(UserDetailsResponse.class);
    }
}
