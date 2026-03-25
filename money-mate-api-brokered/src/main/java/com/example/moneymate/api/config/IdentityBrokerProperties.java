package com.example.moneymate.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity-broker")
public class IdentityBrokerProperties {

    private String issuerUri = "http://localhost:9000";
    private String tokenUri = "http://localhost:9000/oauth2/token";
    private String tokenExchangeClientId = "money-mate-api-brokered";
    private String tokenExchangeClientSecret = "money-mate-api-brokered-secret";

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getTokenExchangeClientId() {
        return tokenExchangeClientId;
    }

    public void setTokenExchangeClientId(String tokenExchangeClientId) {
        this.tokenExchangeClientId = tokenExchangeClientId;
    }

    public String getTokenExchangeClientSecret() {
        return tokenExchangeClientSecret;
    }

    public void setTokenExchangeClientSecret(String tokenExchangeClientSecret) {
        this.tokenExchangeClientSecret = tokenExchangeClientSecret;
    }
}
