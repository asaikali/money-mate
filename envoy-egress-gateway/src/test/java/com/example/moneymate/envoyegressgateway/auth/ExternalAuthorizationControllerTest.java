package com.example.moneymate.envoyegressgateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExternalAuthorizationControllerTest {

    @Test
    void authorizeReturnsDirectLoginHeaderAfterTokenExchange() throws Exception {
        IdentityBrokerTokenExchangeService tokenExchangeService = mock(IdentityBrokerTokenExchangeService.class);
        IdentityBrokerProperties properties = new IdentityBrokerProperties();
        when(tokenExchangeService.exchangeForObpToken("subject-token")).thenReturn("obp-token");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new ExternalAuthorizationController(tokenExchangeService, properties)
        ).build();

        mockMvc.perform(post("/authorize")
                .header("x-authz-secret", properties.getSharedSecret())
                .header(HttpHeaders.AUTHORIZATION, "Bearer subject-token"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "DirectLogin token=obp-token"));
    }

    @Test
    void authorizeRejectsRequestsWithoutSharedSecret() throws Exception {
        IdentityBrokerTokenExchangeService tokenExchangeService = mock(IdentityBrokerTokenExchangeService.class);
        IdentityBrokerProperties properties = new IdentityBrokerProperties();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new ExternalAuthorizationController(tokenExchangeService, properties)
        ).build();

        mockMvc.perform(post("/authorize")
                .header(HttpHeaders.AUTHORIZATION, "Bearer subject-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(tokenExchangeService);
    }
}
