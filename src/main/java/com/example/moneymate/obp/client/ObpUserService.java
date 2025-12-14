package com.example.moneymate.obp.client;

import com.example.moneymate.obp.dto.user.UserDetailsResponse;
import com.example.moneymate.properties.ObpProperties;
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
