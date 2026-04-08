package com.example.moneymate.envoyegressgateway.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalAuthorizationController {

    private final IdentityBrokerTokenExchangeService tokenExchangeService;
    private final IdentityBrokerProperties identityBrokerProperties;

    public ExternalAuthorizationController(IdentityBrokerTokenExchangeService tokenExchangeService,
                                           IdentityBrokerProperties identityBrokerProperties) {
        this.tokenExchangeService = tokenExchangeService;
        this.identityBrokerProperties = identityBrokerProperties;
    }

    @RequestMapping({"/authorize", "/authorize/**"})
    public ResponseEntity<Void> authorize(
        @RequestHeader(value = "x-authz-secret", required = false) String sharedSecret,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        if (!identityBrokerProperties.getSharedSecret().equals(sharedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String subjectToken = extractBearerToken(authorizationHeader);
        if (subjectToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String obpToken;
        try {
            obpToken = tokenExchangeService.exchangeForObpToken(subjectToken);
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "DirectLogin token=" + obpToken)
            .build();
    }

    private String extractBearerToken(String header) {
        if (header == null) {
            return null;
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
