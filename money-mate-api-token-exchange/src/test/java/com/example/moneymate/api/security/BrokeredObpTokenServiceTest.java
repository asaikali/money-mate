package com.example.moneymate.api.security;

import com.example.moneymate.api.config.IdentityBrokerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("BrokeredObpTokenService")
class BrokeredObpTokenServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("exchanges the current JWT for an OBP token at the broker token endpoint")
    void currentObpTokenExchangesBearerToken() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        IdentityBrokerProperties identityBrokerProperties = new IdentityBrokerProperties();
        BrokeredObpTokenService service = new BrokeredObpTokenService(restClientBuilder, identityBrokerProperties);

        SecurityContextHolder.getContext().setAuthentication(jwtAuthentication("subject-token"));

        server.expect(requestTo("http://localhost:9000/oauth2/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic bW9uZXktbWF0ZS1hcGktdG9rZW4tZXhjaGFuZ2U6bW9uZXktbWF0ZS1hcGktdG9rZW4tZXhjaGFuZ2Utc2VjcmV0"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("subject_token=subject-token")))
            .andRespond(withSuccess("""
                {"access_token":"obp-token","token_type":"Bearer","issued_token_type":"urn:ietf:params:oauth:token-type:access_token"}
                """, MediaType.APPLICATION_JSON));

        assertThat(service.currentObpToken()).isEqualTo("obp-token");
        server.verify();
    }

    private static JwtAuthenticationToken jwtAuthentication(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("sub", "katja.fi.29@example.com")
            .claim("aud", List.of("money-mate-api-token-exchange"))
            .build();
        return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
    }
}
