package com.example.moneymate.identitybroker.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthorizationServerConfig")
class AuthorizationServerConfigTest {

    @Test
    @DisplayName("maps protected-test-api client to protected-test-api audience")
    void mapsProtectedTestApiClientAudience() {
        assertThat(AuthorizationServerConfig.resolveAudienceForClientId("protected-test-api-hypermedia-client"))
            .isEqualTo("protected-test-api");
    }

    @Test
    @DisplayName("maps money-mate client to money-mate-api audience")
    void mapsMoneyMateClientAudience() {
        assertThat(AuthorizationServerConfig.resolveAudienceForClientId("money-mate-hypermedia-client"))
            .isEqualTo("money-mate-api");
    }

    @Test
    @DisplayName("rejects unknown client id audience mapping")
    void rejectsUnknownClientAudience() {
        assertThatThrownBy(() -> AuthorizationServerConfig.resolveAudienceForClientId("unknown-client"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No audience configured");
    }

    @Test
    @DisplayName("uses default multi-audience token for unknown client ids")
    void usesDefaultAudiencesForUnknownClient() {
        assertThat(AuthorizationServerConfig.resolveAudiencesForClientId(
            "goose-dcr-client",
            List.of("http://localhost:9091", "money-mate-api-token-exchange")
        )).containsExactly("http://localhost:9091", "money-mate-api-token-exchange");
    }

    @Test
    @DisplayName("uses default multi-audience token for intellij http client")
    void usesDefaultAudiencesForIntellijHttpClient() {
        assertThat(AuthorizationServerConfig.resolveAudiencesForClientId(
            "intellij-http-client",
            List.of("http://localhost:9091", "money-mate-api-token-exchange")
        )).containsExactly("http://localhost:9091", "money-mate-api-token-exchange");
    }
}
