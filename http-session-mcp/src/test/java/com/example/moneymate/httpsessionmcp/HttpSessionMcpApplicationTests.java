package com.example.moneymate.httpsessionmcp;

import com.example.moneymate.httpsessionmcp.tools.HttpGatewayError;
import com.example.moneymate.httpsessionmcp.tools.HttpResponse;
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
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(HttpSessionMcpApplicationTests.TestConfig.class)
class HttpSessionMcpApplicationTests {

    @Autowired
    private HttpSessionTools httpSessionTools;

    @Autowired
    private MockMvc mockMvc;

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

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

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

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

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

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

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

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.body();
        assertThat(body).containsEntry("authorizationPresent", false);
        assertThat(String.valueOf(body.get("accept"))).contains("application/json");
    }

    @Test
    void httpGetForwardsAuthenticatedBearerToken() {
        Jwt jwt = Jwt.withTokenValue("forward-me")
            .header("alg", "none")
            .claim("sub", "demo")
            .claim("aud", List.of("http://localhost:9091"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES)
        );

        try {
            Object result = httpSessionTools.httpGet(localUrl("/test/forwarded-auth"), null);

            assertThat(result).isInstanceOf(HttpResponse.class);
            HttpResponse response = (HttpResponse) result;
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.body();
            assertThat(body).containsEntry("authorization", "Bearer forward-me");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void httpPostReturnsJsonBodyAsObject() {
        Object result = httpSessionTools.httpPost(
            localUrl("/test/post-json"),
            Map.of("Accept", "application/json"),
            Map.of("message", "posted")
        );

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.body();
        assertThat(body).containsEntry("message", "posted");
    }

    @Test
    void httpPostReturnsTextBodyAsString() {
        Object result = httpSessionTools.httpPost(
            localUrl("/test/post-text"),
            Map.of(
                "Accept", "text/plain",
                "Content-Type", "text/plain"
            ),
            "posted text"
        );

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers()).containsEntry("content-type", "text/plain;charset=UTF-8");
        assertThat(response.body()).isEqualTo("posted text");
    }

    @Test
    void httpPostIgnoresAuthorizationHeader() {
        Object result = httpSessionTools.httpPost(
            localUrl("/test/post-headers"),
            Map.of(
                "Authorization", "Bearer should-not-pass",
                "Content-Type", "application/json"
            ),
            Map.of("message", "body")
        );

        assertThat(result).isInstanceOf(HttpResponse.class);
        HttpResponse response = (HttpResponse) result;

        assertThat(response.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.body();
        assertThat(body).containsEntry("authorizationPresent", false);
        assertThat(body).containsEntry("message", "body");
    }

    @Test
    void mcpEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/mcp"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpEndpointAcceptsAuthenticatedJwt() throws Exception {
        mockMvc.perform(get("/mcp")
                .with(jwt().jwt(jwt -> jwt
                    .claim("aud", List.of("http://localhost:9091"))
                    .claim("scope", "hypermedia.access"))))
            .andExpect(result ->
                assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
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

        @Bean
        @Order(0)
        SecurityFilterChain testApiSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/test/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
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

        @GetMapping(value = "/test/forwarded-auth", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> forwardedAuth(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
        ) {
            return Map.of("authorization", authorization == null ? "" : authorization);
        }

        @PostMapping(value = "/test/post-json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> postJson(@RequestBody Map<String, Object> body) {
            return body;
        }

        @PostMapping(value = "/test/post-text", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
        String postText(@RequestBody String body) {
            return body;
        }

        @PostMapping(value = "/test/post-headers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> postHeaders(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody Map<String, Object> body
        ) {
            Map<String, Object> response = new java.util.LinkedHashMap<>(body);
            response.put("authorizationPresent", authorization != null);
            return response;
        }
    }
}
