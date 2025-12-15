package com.example.moneymate.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UuidBearerTokenAuthFilter bearerTokenFilter)
        throws Exception {

        http
            // Stateless API: no server-side HTTP session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Non-browser client: disable CSRF (only safe if you are not using cookies for auth)
            .csrf(csrf -> csrf.disable())

            // Add our Bearer token auth filter
            .addFilterBefore(bearerTokenFilter,
                UsernamePasswordAuthenticationFilter.class)

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/AGENTS.md", "/docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/session").permitAll()
                // logout requires auth (DELETE /session)
                .requestMatchers(HttpMethod.DELETE, "/session").authenticated()
                .anyRequest().authenticated()
            )

            // Return 401 (not redirect) when unauthenticated
            .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            }));

        return http.build();
    }
}
