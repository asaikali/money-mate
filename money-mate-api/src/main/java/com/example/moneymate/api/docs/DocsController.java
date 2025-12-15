package com.example.moneymate.api.docs;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docs")
public class DocsController {

    @GetMapping("/session")
    public ResponseEntity<String> getSessionDocs() {
        String docs = """
            # Session semantics

            This document explains what a **session** represents in this API and how an
            agent must interact with it.

            ## What a session is
            A session represents an **authenticated interaction state** between the client
            and the API. When a session exists, requests may access protected resources
            according to the links and templates exposed by the API.

            A session is created **only** by executing the login operation exposed via a
            HAL-FORMS template that targets `POST /session`.

            ## Access token usage
            When a session is created, the API returns an opaque access token.

            You MUST include this token on all subsequent authenticated requests using the
            HTTP header:

            ```
            Authorization: Bearer <access_token>
            ```

            The access token has no meaning outside this API and MUST NOT be interpreted or
            decoded by the client.

            ## Navigating after authentication
            After creating a session, the API will expose links such as:

            - `self` — the session resource
            - `me` — the authenticated principal
            - `root` — the API entrypoint

            You MUST navigate using only the relations provided in `_links`.

            ## Logging out
            A session is terminated **only** by executing the logout operation exposed via a
            HAL-FORMS template on the session resource that targets `DELETE /session`.

            If no logout template is present, logout is not available in the current state.

            ## Session expiration
            If a session token is missing, invalid, or expired, the API will respond with
            `401 Unauthorized`.

            When this occurs, you MUST return to the API root and re-authenticate using the
            hypermedia controls provided there.

            ## Authority
            This document defines the semantics of the session resource.

            At all times, the authoritative source of what actions are permitted is the
            current API response, as expressed through `_links` and `_templates`.
            """;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/markdown;charset=utf-8")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .body(docs);
    }
}
