package com.example.moneymate.identitybroker.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authorization Server Metadata")
class AuthorizationServerMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OIDC discovery metadata is public and exposes device endpoint")
    void metadataExposesDeviceAuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.device_authorization_endpoint")
                .value("http://localhost:9000/oauth2/device_authorization"));
    }
}
