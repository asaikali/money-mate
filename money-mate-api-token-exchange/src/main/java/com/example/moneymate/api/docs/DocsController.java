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

            This document explains how authentication works in the brokered variant of
            the Money Mate API.

            ## Bearer token access
            This API expects an OAuth 2.0 Bearer token issued by the identity broker.

            You MUST include the token on authenticated requests using the
            HTTP header:

            ```
            Authorization: Bearer <access_token>
            ```

            ## No direct login resource
            This API does not expose `POST /session` or any username/password login flow.
            Authentication is expected to happen outside the API through the configured
            identity broker.

            ## Protected resource metadata
            The server publishes OAuth protected resource metadata so clients can discover
            the associated authorization server and required scope.

            ## Downstream access
            This brokered API may exchange the incoming Bearer token for downstream
            banking access on behalf of the authenticated subject.

            ## Expiration and renewal
            If the Bearer token is missing, invalid, or expired, the API will respond with
            `401 Unauthorized`.

            When this occurs, the client should re-authenticate with the identity broker
            and retry with a new token.
            """;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/markdown;charset=utf-8")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .body(docs);
    }
}
