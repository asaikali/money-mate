package com.example.moneymate.api.obp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "obp")
public record ObpProperties(
    ApiProperties api,
    AuthProperties auth
) {
    public record ApiProperties(
        String baseUrl,
        String version,
        TimeoutProperties timeout
    ) {
        public String buildUrl(String path) {
            return baseUrl + "/obp/" + version + path;
        }
    }

    public record TimeoutProperties(
        Duration connect,
        Duration read
    ) {}

    public record AuthProperties(
        String consumerKey
    ) {}
}
