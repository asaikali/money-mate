package com.example.moneymate.api.obp.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DirectLoginObpClientTest {

    private static final String BASE_URL = "https://apisandbox.openbankproject.com";

    @Test
    void acceptsJsonTokenLabelledAsTextPlain() {
        TestFixture fixture = fixture();
        fixture.server().expect(requestTo(BASE_URL + "/my/logins/direct"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(
                HttpHeaders.AUTHORIZATION,
                "DirectLogin username=user@example.com,password=test-password,consumer_key=consumer-key"
            ))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.TEXT_PLAIN)
                .body("""
                    {
                      "token":"obp-token"
                    }
                    """));

        assertThat(fixture.client().login("user@example.com", "test-password"))
            .isEqualTo("obp-token");
        fixture.server().verify();
    }

    @Test
    void retainsObpJsonAuthenticationError() {
        TestFixture fixture = fixture();
        fixture.server().expect(requestTo(BASE_URL + "/my/logins/direct"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":401,\"message\":\"OBP-20006: Invalid login credentials.\"}"));

        assertThatThrownBy(() -> fixture.client().login("user@example.com", "wrong"))
            .isInstanceOf(ObpAuthenticationException.class)
            .hasMessage("OBP-20006: Invalid login credentials.")
            .satisfies(exception -> {
                ObpAuthenticationException authenticationException = (ObpAuthenticationException) exception;
                assertThat(authenticationException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(authenticationException.getResponseBody()).contains("Invalid login credentials");
            });
        fixture.server().verify();
    }

    @Test
    void reportsMalformedSuccessfulResponseAsObpServiceFailure() {
        TestFixture fixture = fixture();
        fixture.server().expect(requestTo(BASE_URL + "/my/logins/direct"))
            .andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.TEXT_PLAIN)
                .body("not-json"));

        assertThatThrownBy(() -> fixture.client().login("user@example.com", "test-password"))
            .isInstanceOf(ObpClientException.class)
            .isNotInstanceOf(ObpAuthenticationException.class)
            .hasMessage("OBP DirectLogin returned invalid JSON");
        fixture.server().verify();
    }

    private static TestFixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DirectLoginObpClient client = new DirectLoginObpClient(
            builder.build(),
            properties(),
            JsonMapper.builder().build()
        );
        return new TestFixture(client, server);
    }

    private static ObpProperties properties() {
        return new ObpProperties(
            new ObpProperties.ApiProperties(
                BASE_URL,
                "v5.1.0",
                new ObpProperties.TimeoutProperties(Duration.ofSeconds(10), Duration.ofSeconds(30))
            ),
            new ObpProperties.AuthProperties("consumer-key")
        );
    }

    private record TestFixture(DirectLoginObpClient client, MockRestServiceServer server) {
    }
}
