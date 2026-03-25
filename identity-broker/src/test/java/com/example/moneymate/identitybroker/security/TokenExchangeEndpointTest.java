package com.example.moneymate.identitybroker.security;

import com.example.moneymate.identitybroker.obp.ObpDirectLoginService;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Token Exchange Endpoint")
class TokenExchangeEndpointTest {

    private static final String CLIENT_ID = "money-mate-api-brokered";
    private static final String CLIENT_SECRET = "money-mate-api-brokered-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> jwkSource;

    @MockitoBean
    private ObpDirectLoginService obpDirectLoginService;

    @Test
    @DisplayName("POST /oauth2/token exchanges a broker JWT for an OBP token")
    void tokenExchangeReturnsObpToken() throws Exception {
        when(obpDirectLoginService.login("katja.fi.29@example.com", "ca0317"))
            .thenReturn("obp-direct-login-token");

        String subjectToken = subjectToken("katja.fi.29@example.com");

        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", basicAuth(CLIENT_ID, CLIENT_SECRET))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(
                    "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" +
                        "&subject_token=" + subjectToken +
                        "&subject_token_type=urn:ietf:params:oauth:token-type:access_token" +
                        "&requested_token_type=urn:ietf:params:oauth:token-type:access_token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("obp-direct-login-token"))
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.issued_token_type").value("urn:ietf:params:oauth:token-type:access_token"));
    }

    private String subjectToken(String subject) {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        Instant issuedAt = Instant.now();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("http://localhost:9000")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .audience(List.of("money-mate-api-brokered"))
                .claim("scope", "hypermedia.access")
                .build()))
            .getTokenValue();
    }

    private static String basicAuth(String username, String password) {
        String value = Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + value;
    }
}
