package com.example.moneymate.api.security;

import com.example.moneymate.api.config.IdentityBrokerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(IdentityBrokerProperties.class)
public class SecurityConfig {

    private static final String REQUIRED_SCOPE = "hypermedia.access";

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        IdentityBrokerProperties identityBrokerProperties,
        AuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/", "/AGENTS.md", "/docs/**", "/.well-known/oauth-protected-resource").permitAll()
                .anyRequest().hasAuthority("SCOPE_" + REQUIRED_SCOPE)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint)
                .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(builder -> builder
                    .authorizationServer(identityBrokerProperties.getIssuerUri())
                    .scope(REQUIRED_SCOPE)
                    .resourceName("money-mate-api-brokered"))))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return new BearerTokenAuthenticationEntryPoint();
    }
}
