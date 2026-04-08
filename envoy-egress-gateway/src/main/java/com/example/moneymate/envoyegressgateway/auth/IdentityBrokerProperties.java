package com.example.moneymate.envoyegressgateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity-broker")
public class IdentityBrokerProperties {

    private String tokenUri = "http://localhost:9000/oauth2/token";
    private String tokenExchangeClientId = "money-mate-api-token-exchange";
    private String tokenExchangeClientSecret = "money-mate-api-token-exchange-secret";
    private String sharedSecret = "shared-secret-for-authz";

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

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
