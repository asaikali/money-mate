package com.example.moneymate.api.root;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ApiRootController using MockMvc.
 * Validates HTTP response structure and hypermedia controls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Root Endpoint Tests")
class ApiRootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / should return 200 OK with HAL+JSON content type")
    void getRoot_shouldReturn200WithHalJson() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/prs.hal-forms+json"));
    }

    @Test
    @DisplayName("GET / should include agent_bootstrap field with instructions")
    void getRoot_shouldIncludeAgentBootstrapField() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agent_bootstrap").exists())
            .andExpect(jsonPath("$.agent_bootstrap").isString())
            .andExpect(jsonPath("$.agent_bootstrap").value(notNullValue()));
    }

    @Test
    @DisplayName("GET / should include self link pointing to root")
    void getRoot_shouldIncludeSelfLink() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.self.href").value("http://localhost/"));
    }

    @Test
    @DisplayName("GET / should include profile link to AGENTS.md with metadata")
    void getRoot_shouldIncludeProfileLink() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.profile").exists())
            .andExpect(jsonPath("$._links.profile.href").value("/AGENTS.md"))
            .andExpect(jsonPath("$._links.profile.type").value("text/markdown"))
            .andExpect(jsonPath("$._links.profile.title").value("Agent Instructions - MUST READ"));
    }

    @Test
    @DisplayName("GET / should expose self, profile, and about when unauthenticated")
    void getRoot_shouldExposePublicLinksWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.profile").exists())
            .andExpect(jsonPath("$._links.about").exists())
            .andExpect(jsonPath("$._links.me").doesNotExist())
            .andExpect(jsonPath("$._links.accounts").doesNotExist())
            .andExpect(jsonPath("$._links.*").isArray())
            .andExpect(jsonPath("$._links.*").value(hasSize(3)));
    }

    @Test
    @DisplayName("GET / should expose me and accounts when authenticated")
    void getRoot_shouldExposeAuthenticatedLinks() throws Exception {
        mockMvc.perform(get("/")
                .with(jwt().jwt(jwt -> jwt
                    .claim("aud", List.of("money-mate-api-brokered"))
                    .claim("scope", "hypermedia.access"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.me").exists())
            .andExpect(jsonPath("$._links.accounts").exists())
            .andExpect(jsonPath("$._links.session").doesNotExist());
    }

    @Test
    @DisplayName("GET /users/me should require bearer authentication")
    void getProtectedEndpoint_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /AGENTS.md should return 200 OK with markdown content")
    void getAgentsMd_shouldReturn200WithTextMarkdown() throws Exception {
        mockMvc.perform(get("/AGENTS.md"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/markdown"))
            .andExpect(content().string(notNullValue()));
    }
}
