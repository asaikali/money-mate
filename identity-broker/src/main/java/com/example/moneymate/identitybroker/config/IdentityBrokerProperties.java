package com.example.moneymate.identitybroker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "identity-broker")
public class IdentityBrokerProperties {

    private String issuerUri = "http://localhost:9000";
    private List<String> defaultAudiences = new ArrayList<>(List.of(
        "http://localhost:9091",
        "money-mate-api-token-exchange"
    ));
    private final List<DemoUser> demoUsers = new ArrayList<>();
    private final TokenExchangeClient tokenExchangeClient = new TokenExchangeClient();
    private final IntellijHttpClient intellijHttpClient = new IntellijHttpClient();

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public List<String> getDefaultAudiences() {
        return defaultAudiences;
    }

    public void setDefaultAudiences(List<String> defaultAudiences) {
        this.defaultAudiences = defaultAudiences;
    }

    public List<DemoUser> getDemoUsers() {
        return demoUsers;
    }

    public TokenExchangeClient getTokenExchangeClient() {
        return tokenExchangeClient;
    }

    public IntellijHttpClient getIntellijHttpClient() {
        return intellijHttpClient;
    }

    public DemoUser findDemoUser(String username) {
        return demoUsers.stream()
            .filter(user -> user.getUsername().equals(username))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No demo user configured for username: " + username));
    }

    public static class DemoUser {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class TokenExchangeClient {
        private String clientId = "money-mate-api-token-exchange";
        private String clientSecret = "money-mate-api-token-exchange-secret";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static class IntellijHttpClient {
        private String clientId = "intellij-http-client";
        private String redirectUri = "http://127.0.0.1:63342/api/http-client/oauth2/callback";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
