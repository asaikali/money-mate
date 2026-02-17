package com.example.moneymate.identitybroker.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Device Authorization Endpoint")
class DeviceAuthorizationEndpointTest {
    private static final String TEST_API_CLIENT_ID = "protected-test-api-hypermedia-client";
    private static final String MONEY_MATE_CLIENT_ID = "money-mate-hypermedia-client";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /oauth2/device_authorization returns JSON for protected-test-api client")
    void deviceAuthorizationReturnsJsonForProtectedTestApiClient() throws Exception {
        mockMvc.perform(post("/oauth2/device_authorization")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("client_id=" + TEST_API_CLIENT_ID + "&scope=hypermedia.access"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.device_code").exists())
            .andExpect(jsonPath("$.user_code").exists())
            .andExpect(jsonPath("$.verification_uri").exists())
            .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    @DisplayName("POST /oauth2/device_authorization returns JSON for money-mate client")
    void deviceAuthorizationReturnsJsonForMoneyMateClient() throws Exception {
        mockMvc.perform(post("/oauth2/device_authorization")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("client_id=" + MONEY_MATE_CLIENT_ID + "&scope=hypermedia.access"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.device_code").exists())
            .andExpect(jsonPath("$.user_code").exists())
            .andExpect(jsonPath("$.verification_uri").exists())
            .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    @DisplayName("POST /oauth2/token for device code is accepted for protected-test-api client")
    void tokenEndpointAcceptsProtectedTestApiClientDeviceCodeRequest() throws Exception {
        MvcResult deviceAuthorizationResult = mockMvc.perform(post("/oauth2/device_authorization")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("client_id=" + TEST_API_CLIENT_ID + "&scope=hypermedia.access"))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = deviceAuthorizationResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String deviceCode = extractJsonField(responseBody, "device_code");

        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("grant_type=urn:ietf:params:oauth:grant-type:device_code"
                    + "&device_code=" + deviceCode
                    + "&client_id=" + TEST_API_CLIENT_ID))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("authorization_pending"));
    }

    private static String extractJsonField(String json, String field) {
        int fieldStart = json.indexOf("\"" + field + "\"");
        if (fieldStart < 0) {
            throw new IllegalStateException("Field not found in JSON: " + field);
        }
        int colonIndex = json.indexOf(':', fieldStart);
        int firstQuote = json.indexOf('"', colonIndex + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (colonIndex < 0 || firstQuote < 0 || secondQuote < 0) {
            throw new IllegalStateException("Invalid JSON structure for field: " + field);
        }
        return json.substring(firstQuote + 1, secondQuote);
    }
}
