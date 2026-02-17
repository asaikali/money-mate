package com.example.moneymate.protectedtestapi.protectedresource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProtectedController")
class ProtectedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /protected without token returns Bearer challenge with resource metadata reference")
    void protectedWithoutTokenReturns401WithBearerHeader() throws Exception {
        mockMvc.perform(get("/protected"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                containsString("resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\"")));
    }

    @Test
    @DisplayName("GET /protected with valid scope returns diagnostics")
    void protectedWithScopeReturnsDiagnostics() throws Exception {
        mockMvc.perform(get("/protected")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .subject("user-123")
                        .claim("preferred_username", "demo")
                        .claim("scope", "hypermedia.access")
                        .audience(java.util.List.of("protected-test-api")))
                    .authorities(new SimpleGrantedAuthority("SCOPE_hypermedia.access"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Protected endpoint reached with a valid access token."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.user.subject").value("user-123"))
            .andExpect(jsonPath("$.user.preferredUsername").value("demo"))
            .andExpect(jsonPath("$.user.audience[0]").value("protected-test-api"))
            .andExpect(jsonPath("$.user.scope").value("hypermedia.access"));
    }
}
