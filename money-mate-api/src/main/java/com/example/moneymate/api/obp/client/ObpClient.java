package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ObpClient {

    private static final Logger log = LoggerFactory.getLogger(ObpClient.class);

    private final RestClient publicRestClient;
    private final String consumerKey;
    private final String apiVersion;

    public ObpClient(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties
    ) {
        this.publicRestClient = publicRestClient;
        this.consumerKey = properties.auth().consumerKey();
        this.apiVersion = properties.api().version();
    }

    /**
     * Authenticate user with OBP DirectLogin and return OBP token.
     *
     * @param username User's email/username
     * @param password User's password
     * @return OBP DirectLogin token
     * @throws ObpAuthenticationException if authentication fails
     * @throws ObpClientException if OBP is unreachable or returns error
     */
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

    /**
     * Get current user details from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @return User details from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public UserDetailsResponse getCurrentUser(String obpToken) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/users/current";

        log.debug("Fetching current user from OBP");

        try {
            UserDetailsResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(UserDetailsResponse.class);

            if (response == null) {
                log.error("OBP users/current returned null response");
                throw new ObpClientException("Failed to fetch user details from OBP");
            }

            log.debug("Successfully fetched user details for: {}", response.username());
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch user details from OBP: {}", e.getMessage(), e);
            throw new ObpClientException("Failed to fetch user details from OBP", e);
        }
    }
}
