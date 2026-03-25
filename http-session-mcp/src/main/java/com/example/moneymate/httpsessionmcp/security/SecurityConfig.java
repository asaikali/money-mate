package com.example.moneymate.httpsessionmcp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.OAuth2ProtectedResourceMetadata;
import org.springframework.security.web.SecurityFilterChain;

import java.util.function.Consumer;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Value("${http-session-mcp.authorization-server:http://localhost:9000}") String authorizationServer
    ) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/mcp", "/mcp/**").authenticated()
                .anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/mcp", "/mcp/**"))
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
                .protectedResourceMetadata(protectedResourceMetadata ->
                    protectedResourceMetadata.protectedResourceMetadataCustomizer(
                        protectedResourceMetadataCustomizer(authorizationServer))));

        return http.build();
    }

    private static Consumer<OAuth2ProtectedResourceMetadata.Builder> protectedResourceMetadataCustomizer(
        String authorizationServer
    ) {
        return builder -> builder
            .authorizationServer(authorizationServer)
            .scope("hypermedia.access")
            .resourceName("http-session-mcp");
    }
}
