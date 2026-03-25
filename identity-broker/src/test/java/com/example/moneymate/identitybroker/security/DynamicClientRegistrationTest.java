package com.example.moneymate.identitybroker.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Dynamic Client Registration")
class DynamicClientRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    @DisplayName("POST /oauth2/register allows open registration for a public client")
    void openRegistrationCreatesClient() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "client_name": "Goose MCP Client",
                      "redirect_uris": ["http://127.0.0.1:8080/callback"],
                      "grant_types": ["authorization_code", "refresh_token"],
                      "response_types": ["code"],
                      "token_endpoint_auth_method": "none",
                      "scope": "hypermedia.access"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.client_id").isNotEmpty())
            .andExpect(jsonPath("$.client_name").value("Goose MCP Client"))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        String clientId = extractJsonField(response, "client_id");

        assertThat(registeredClientRepository.findByClientId(clientId)).isNotNull();
    }

    private static String extractJsonField(String json, String field) {
        int fieldStart = json.indexOf("\"" + field + "\"");
        int colonIndex = json.indexOf(':', fieldStart);
        int firstQuote = json.indexOf('"', colonIndex + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }
}
