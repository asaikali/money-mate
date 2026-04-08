package com.example.moneymate.envoyegressgateway;

import com.example.moneymate.envoyegressgateway.auth.IdentityBrokerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IdentityBrokerProperties.class)
public class EnvoyEgressGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvoyEgressGatewayApplication.class, args);
    }
}
