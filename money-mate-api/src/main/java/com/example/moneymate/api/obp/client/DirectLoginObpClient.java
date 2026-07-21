package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DirectLoginObpClient extends ObpClient {

    private static final Logger log = LoggerFactory.getLogger(DirectLoginObpClient.class);
    private static final Pattern OBP_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private final String consumerKey;
    private final ObjectMapper objectMapper;

    public DirectLoginObpClient(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties,
        ObjectMapper objectMapper
    ) {
        super(publicRestClient, properties);
        this.consumerKey = properties.auth().consumerKey();
        this.objectMapper = objectMapper;
    }

    @Override
    protected void applyAuth(RestClient.RequestHeadersSpec<?> spec, String token) {
        spec.header(HttpHeaders.AUTHORIZATION, "DirectLogin token=" + token);
    }

    public String login(String username, String password) {
        String directLoginHeader = String.format(
            "DirectLogin username=%s,password=%s,consumer_key=%s",
            username, password, consumerKey
        );

        log.debug("Attempting OBP DirectLogin for user: {}", username);

        try {
            String responseBody = publicRestClient.post()
                .uri("/my/logins/direct")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, directLoginHeader)
                .body("{}")
                .retrieve()
                .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.error("OBP DirectLogin returned an empty response for user: {}", username);
                throw new ObpClientException("OBP DirectLogin returned an empty response");
            }

            DirectLoginResponse response = objectMapper.readValue(responseBody, DirectLoginResponse.class);

            if (response.token() == null || response.token().isBlank()) {
                log.error("OBP DirectLogin returned null response or token for user: {}", username);
                throw new ObpAuthenticationException("OBP authentication failed: no token received");
            }

            log.info("Successfully authenticated user: {}", username);
            return response.token();

        } catch (RestClientResponseException e) {
            String obpMessage = extractObpMessage(e);
            log.error("OBP DirectLogin failed for user {}: {}", username, obpMessage, e);
            throw new ObpAuthenticationException(
                obpMessage,
                e.getStatusCode(),
                e.getResponseBodyAsString()
            );
        } catch (JacksonException e) {
            log.error("OBP DirectLogin returned invalid JSON for user {}", username, e);
            throw new ObpClientException("OBP DirectLogin returned invalid JSON", e);
        } catch (RestClientException e) {
            log.error("OBP DirectLogin failed for user {}: {}", username, e.getMessage(), e);
            throw new ObpAuthenticationException("OBP authentication failed", e);
        }
    }

    private String extractObpMessage(RestClientResponseException e) {
        String responseBody = e.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return "OBP authentication failed";
        }

        Matcher matcher = OBP_MESSAGE_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return responseBody;
    }
}
