package com.example.moneymate.identitybroker.obp;

import com.example.moneymate.identitybroker.security.TokenExchangeAuthenticationException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ObpDirectLoginService {

    private final RestClient restClient;
    private final String consumerKey;

    public ObpDirectLoginService(ObpBrokerProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getApi().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getApi().getReadTimeout());

        this.restClient = RestClient.builder()
            .baseUrl(properties.getApi().getBaseUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(requestFactory)
            .build();
        this.consumerKey = properties.getAuth().getConsumerKey();
    }

    public String login(String username, String password) {
        String directLoginHeader = String.format(
            "username=%s, password=%s, consumer_key=%s",
            username, password, consumerKey
        );

        try {
            DirectLoginResponse response = restClient.post()
                .uri("/my/logins/direct")
                .header("directlogin", directLoginHeader)
                .body("{}")
                .retrieve()
                .body(DirectLoginResponse.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new TokenExchangeAuthenticationException("invalid_grant", "OBP DirectLogin did not return a token");
            }

            return response.token();
        } catch (RestClientException exception) {
            throw new TokenExchangeAuthenticationException("invalid_grant", "OBP DirectLogin failed", exception);
        }
    }

    private record DirectLoginResponse(String token) {
    }
}
