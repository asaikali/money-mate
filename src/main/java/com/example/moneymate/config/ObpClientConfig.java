package com.example.moneymate.config;

import com.example.moneymate.obp.client.ObpAuthenticationService;
import com.example.moneymate.obp.interceptor.DirectLoginInterceptor;
import com.example.moneymate.properties.ObpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ObpProperties.class)
public class ObpClientConfig {

    @Bean("obpPublicRestClient")
    public RestClient obpPublicRestClient(RestClient.Builder builder, ObpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.api().timeout().connect());
        requestFactory.setReadTimeout(properties.api().timeout().read());

        return builder
            .baseUrl(properties.api().baseUrl())
            .defaultHeader("Content-Type", "application/json")
            .requestFactory(requestFactory)
            .build();
    }

    @Bean("obpAuthenticatedRestClient")
    public RestClient obpAuthenticatedRestClient(
        RestClient.Builder builder,
        ObpAuthenticationService authService,
        ObpProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.api().timeout().connect());
        requestFactory.setReadTimeout(properties.api().timeout().read());

        return builder
            .baseUrl(properties.api().baseUrl())
            .defaultHeader("Content-Type", "application/json")
            .requestFactory(requestFactory)
            .requestInterceptor(new DirectLoginInterceptor(authService))
            .build();
    }
}
