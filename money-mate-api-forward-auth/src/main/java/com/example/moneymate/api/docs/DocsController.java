package com.example.moneymate.api.docs;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docs")
public class DocsController {

    @GetMapping("/authentication")
    public ResponseEntity<String> getAuthenticationDocs() {
        String docs = """
            # Authentication semantics

            This document explains how authentication works in the forwarded-auth
            variant of the Money Mate API.

            ## Bearer token access
            This API expects an inbound Bearer token on protected requests.

            You MUST include the token on authenticated requests using the
            HTTP header:

            ```
            Authorization: Bearer <access_token>
            ```

            ## No direct login resource
            This API does not expose `POST /session` or any username/password login flow.
            Authentication is expected to happen outside this API.

            ## Downstream access
            This API forwards the incoming Bearer token to its configured egress
            gateway when it makes downstream OBP requests. The egress gateway is
            responsible for any token exchange and any OBP-specific authorization
            header formatting.

            ## Expiration and renewal
            If the Bearer token is missing, the API will respond with
            `401 Unauthorized`.

            If the egress gateway or downstream banking system rejects the forwarded
            token, the API will surface the downstream failure.
            """;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/markdown;charset=utf-8")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .body(docs);
    }
}
