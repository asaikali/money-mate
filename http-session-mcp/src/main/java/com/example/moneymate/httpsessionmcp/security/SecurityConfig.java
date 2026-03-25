package com.example.moneymate.httpsessionmcp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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

    @Bean
    JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
        @Value("${http-session-mcp.resource-identifier:http://localhost:9091}") String resourceIdentifier
    ) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithValidators(
            new JwtAudienceValidator(resourceIdentifier));
        jwtDecoder.setJwtValidator(jwtValidator);
        return jwtDecoder;
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
