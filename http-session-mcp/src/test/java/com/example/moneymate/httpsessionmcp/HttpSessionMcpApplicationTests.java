package com.example.moneymate.httpsessionmcp;

import com.example.moneymate.httpsessionmcp.tools.HttpGatewayError;
import com.example.moneymate.httpsessionmcp.tools.HttpGetResponse;
import com.example.moneymate.httpsessionmcp.tools.HttpSessionTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(HttpSessionMcpApplicationTests.TestConfig.class)
class HttpSessionMcpApplicationTests {

    @Autowired
    private HttpSessionTools httpSessionTools;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(httpSessionTools).isNotNull();
    }

    @Test
    void httpGetReturnsJsonBodyAsObject() {
        Object result = httpSessionTools.httpGet(
            localUrl("/test/json"),
            Map.of("Accept", "application/json")
        );

        assertThat(result).isInstanceOf(HttpGetResponse.class);
        HttpGetResponse response = (HttpGetResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers()).containsKey("content-type");
        assertThat(response.body()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.body();
        assertThat(body).containsEntry("message", "ok");
    }

    @Test
    void httpGetReturnsMarkdownBodyAsString() {
        Object result = httpSessionTools.httpGet(
            localUrl("/test/markdown"),
            Map.of("Accept", "text/markdown")
        );

        assertThat(result).isInstanceOf(HttpGetResponse.class);
        HttpGetResponse response = (HttpGetResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers()).containsEntry("content-type", "text/markdown;charset=UTF-8");
        assertThat(response.body()).isEqualTo("# Example\n\nMarkdown response");
    }

    @Test
    void httpGetReturnsDownstream404AsNormalResponse() {
        Object result = httpSessionTools.httpGet(
            localUrl("/test/missing"),
            Map.of("Accept", "application/json")
        );

        assertThat(result).isInstanceOf(HttpGetResponse.class);
        HttpGetResponse response = (HttpGetResponse) result;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.headers()).containsKey("content-type");
        assertThat(response.body()).isInstanceOf(Map.class);
    }

    @Test
    void httpGetRejectsInvalidUrl() {
        Object result = httpSessionTools.httpGet("/relative/path", null);

        assertThat(result).isInstanceOf(HttpGatewayError.class);
        HttpGatewayError error = (HttpGatewayError) result;

        assertThat(error.error()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void httpGetIgnoresAuthorizationHeader() {
        Object result = httpSessionTools.httpGet(
            localUrl("/test/headers"),
            Map.of(
                "Authorization", "Bearer should-not-pass",
                "Accept", "application/json"
            )
        );

        assertThat(result).isInstanceOf(HttpGetResponse.class);
        HttpGetResponse response = (HttpGetResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.body();
        assertThat(body).containsEntry("authorizationPresent", false);
        assertThat(String.valueOf(body.get("accept"))).contains("application/json");
    }

    private String localUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        TestApiController testApiController() {
            return new TestApiController();
        }
    }

    @RestController
    static class TestApiController {

        @GetMapping(value = "/test/json", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> json() {
            return Map.of("message", "ok");
        }

        @GetMapping(value = "/test/markdown", produces = MediaType.TEXT_MARKDOWN_VALUE)
        String markdown() {
            return "# Example\n\nMarkdown response";
        }

        @GetMapping(value = "/test/headers", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> headers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept
        ) {
            return Map.of(
                "authorizationPresent", authorization != null,
                "accept", accept == null ? "" : accept
            );
        }
    }
}
