package com.example.moneymate.api.obp.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ObpWireLoggingInterceptorTest {

    private final Logger wireLogger = (Logger) LoggerFactory.getLogger(ObpWireLoggingInterceptor.class);
    private Level previousLevel;

    @BeforeEach
    void enableWireLogging() {
        previousLevel = wireLogger.getLevel();
        wireLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void restoreWireLoggingLevel() {
        wireLogger.setLevel(previousLevel);
    }

    @Test
    void logsJsonErrorResponseAndRedactsRequestCredentials(CapturedOutput output) throws Exception {
        MockClientHttpRequest request = directLoginRequest();
        MockClientHttpResponse upstreamResponse = response(
            HttpStatus.UNAUTHORIZED,
            MediaType.APPLICATION_JSON,
            "{\"code\":401,\"message\":\"OBP-20008: Invalid Consumer Key.\"}"
        );

        ClientHttpResponse response = new ObpWireLoggingInterceptor().intercept(
            request,
            "{}".getBytes(StandardCharsets.UTF_8),
            (ignoredRequest, ignoredBody) -> upstreamResponse
        );

        assertThat(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))
            .contains("Invalid Consumer Key");
        assertThat(output)
            .contains("OBP wire request")
            .contains("username=timo.fi.29@example.com")
            .contains("password=<redacted>")
            .contains("consumer_key=<redacted>")
            .contains("OBP wire response")
            .contains("HTTP 401 Unauthorized")
            .contains("Content-Type: application/json")
            .contains("Invalid Consumer Key")
            .doesNotContain("6addcd")
            .doesNotContain("consumer-secret");
    }

    @Test
    void logsTextPlainResponseAndRedactsReturnedToken(CapturedOutput output) throws Exception {
        MockClientHttpRequest request = directLoginRequest();
        MockClientHttpResponse upstreamResponse = response(
            HttpStatus.OK,
            MediaType.TEXT_PLAIN,
            "{\"token\":\"secret-obp-token\"}"
        );

        ClientHttpResponse response = new ObpWireLoggingInterceptor().intercept(
            request,
            "{}".getBytes(StandardCharsets.UTF_8),
            (ignoredRequest, ignoredBody) -> upstreamResponse
        );

        assertThat(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("{\"token\":\"secret-obp-token\"}");
        assertThat(output)
            .contains("HTTP 200 OK")
            .contains("Content-Type: text/plain")
            .contains("{\"token\":\"<redacted>\"}")
            .doesNotContain("secret-obp-token");
    }

    @Test
    void ignoresNonDirectLoginRequests(CapturedOutput output) throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(
            HttpMethod.GET,
            URI.create("https://apisandbox.openbankproject.com/obp/v5.1.0/my/accounts")
        );
        MockClientHttpResponse upstreamResponse = response(
            HttpStatus.OK,
            MediaType.APPLICATION_JSON,
            "{\"accounts\":[{\"balance\":\"sensitive\"}]}"
        );

        new ObpWireLoggingInterceptor().intercept(
            request,
            new byte[0],
            (ignoredRequest, ignoredBody) -> upstreamResponse
        );

        assertThat(output).doesNotContain("sensitive");
    }

    private static MockClientHttpRequest directLoginRequest() {
        MockClientHttpRequest request = new MockClientHttpRequest(
            HttpMethod.POST,
            URI.create("https://apisandbox.openbankproject.com/my/logins/direct")
        );
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        request.getHeaders().set(
            HttpHeaders.AUTHORIZATION,
            "DirectLogin username=timo.fi.29@example.com,password=6addcd,consumer_key=consumer-secret"
        );
        return request;
    }

    private static MockClientHttpResponse response(HttpStatus status, MediaType contentType, String body) {
        MockClientHttpResponse response = new MockClientHttpResponse(
            body.getBytes(StandardCharsets.UTF_8),
            status
        );
        response.getHeaders().setContentType(contentType);
        return response;
    }
}
