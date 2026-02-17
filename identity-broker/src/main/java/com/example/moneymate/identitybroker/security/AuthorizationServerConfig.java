package com.example.moneymate.identitybroker.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.web.authentication.PublicClientAuthenticationConverter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {
    private static final String SCOPE_HYPERMEDIA_ACCESS = "hypermedia.access";
    private static final String TEST_API_CLIENT_ID = "protected-test-api-hypermedia-client";
    private static final String MONEY_MATE_CLIENT_ID = "money-mate-hypermedia-client";
    private static final String TEST_API_AUDIENCE = "protected-test-api";
    private static final String MONEY_MATE_AUDIENCE = "money-mate-api";
    private static final Map<String, String> AUDIENCE_BY_CLIENT_ID = Map.of(
        TEST_API_CLIENT_ID, TEST_API_AUDIENCE,
        MONEY_MATE_CLIENT_ID, MONEY_MATE_AUDIENCE
    );

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
        HttpSecurity http,
        RegisteredClientRepository registeredClientRepository
    ) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
            new OAuth2AuthorizationServerConfigurer();
        MediaTypeRequestMatcher htmlRequestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        htmlRequestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        RequestMatcher browserGetRequestMatcher = new AndRequestMatcher(
            request -> HttpMethod.GET.matches(request.getMethod()),
            htmlRequestMatcher
        );

        http
            .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(authorizationServerConfigurer, authorizationServer -> authorizationServer
                .deviceAuthorizationEndpoint(Customizer.withDefaults())
                .deviceVerificationEndpoint(Customizer.withDefaults())
                .oidc(Customizer.withDefaults())
                .clientAuthentication(clientAuthentication -> clientAuthentication
                    .authenticationConverters(authenticationConverters ->
                        authenticationConverters.add(0, new DeviceFlowPublicClientAuthenticationConverter()))
                    .authenticationConverter(new PublicClientAuthenticationConverter())
                    .authenticationProvider(
                        new DeviceFlowPublicClientAuthenticationProvider(registeredClientRepository))
                ))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                browserGetRequestMatcher
            ));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    UserDetailsManager userDetailsService(
        @Value("${identity-broker.test-user.username:demo}") String username,
        @Value("${identity-broker.test-user.password:demo}") String password
    ) {
        UserDetails user = User.builder()
            .username(username)
            .password("{noop}" + password)
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository() {
        RegisteredClient protectedTestApiClient = buildPublicDeviceClient(TEST_API_CLIENT_ID);
        RegisteredClient moneyMateClient = buildPublicDeviceClient(MONEY_MATE_CLIENT_ID);

        return new InMemoryRegisteredClientRepository(protectedTestApiClient, moneyMateClient);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                String clientId = context.getRegisteredClient().getClientId();
                String audience = resolveAudienceForClientId(clientId);
                context.getClaims().audience(List.of(audience));
            }
        };
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
        @Value("${identity-broker.issuer-uri:http://localhost:9000}") String issuerUri
    ) {
        return AuthorizationServerSettings.builder()
            .issuer(issuerUri)
            .build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate RSA key", exception);
        }
    }

    private static RegisteredClient buildPublicDeviceClient(String clientId) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
            .scope(SCOPE_HYPERMEDIA_ACCESS)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .build())
            .build();
    }

    static String resolveAudienceForClientId(String clientId) {
        String audience = AUDIENCE_BY_CLIENT_ID.get(clientId);
        if (audience == null) {
            throw new IllegalStateException("No audience configured for client_id: " + clientId);
        }
        return audience;
    }
}
