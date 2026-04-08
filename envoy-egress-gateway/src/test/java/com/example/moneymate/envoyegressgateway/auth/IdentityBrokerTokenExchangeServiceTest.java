package com.example.moneymate.envoyegressgateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityBrokerTokenExchangeServiceTest {

    @Test
    void exchangeForObpTokenCallsIdentityBrokerTokenEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        IdentityBrokerProperties properties = new IdentityBrokerProperties();
        IdentityBrokerTokenExchangeService service = new IdentityBrokerTokenExchangeService(builder, properties);

        server.expect(requestTo("http://localhost:9000/oauth2/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic bW9uZXktbWF0ZS1hcGktdG9rZW4tZXhjaGFuZ2U6bW9uZXktbWF0ZS1hcGktdG9rZW4tZXhjaGFuZ2Utc2VjcmV0"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"))
            .andExpect(content().string(containsString("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange")))
            .andExpect(content().string(containsString("subject_token=subject-token")))
            .andRespond(withSuccess("""
                {"access_token":"obp-token","token_type":"Bearer","issued_token_type":"urn:ietf:params:oauth:token-type:access_token"}
                """, MediaType.APPLICATION_JSON));

        assertThat(service.exchangeForObpToken("subject-token")).isEqualTo("obp-token");
        server.verify();
    }
}
