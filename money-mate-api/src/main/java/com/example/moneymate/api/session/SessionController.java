package com.example.moneymate.api.session;

import com.example.moneymate.api.obp.client.ObpAuthenticationException;
import com.example.moneymate.api.obp.client.ObpClient;
import com.example.moneymate.api.obp.client.ObpClientException;
import com.example.moneymate.api.security.SessionTokenStore;
import com.example.moneymate.api.user.UserController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/session")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final SessionTokenStore tokenStore;
    private final ObpClient obpClient;

    public SessionController(SessionTokenStore tokenStore, ObpClient obpClient) {
        this.tokenStore = tokenStore;
        this.obpClient = obpClient;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
        @Valid @RequestBody LoginRequest credentials) {

        try {
            // Authenticate with OBP DirectLogin
            String obpToken = obpClient.login(credentials.username(), credentials.password());

            // Create session with OBP token and generate MMAT token
            String token = tokenStore.create(credentials.username(), obpToken);

            // Build response
            SessionResponse response = SessionResponse.create(token);

            // Build links without affordances - guide agent to navigate, not act
            // After login, agent should follow links to discover available actions
            Link meLink = linkTo(methodOn(UserController.class).getCurrentUser()).withRel("me")
                .withTitle("Your user profile and available actions");

            Link selfLink = linkTo(methodOn(SessionController.class).getSession(null)).withSelfRel();

            Link aboutLink = Link.of("/docs/session", "about")
                .withType("text/markdown")
                .withTitle("Session semantics (MUST READ)");

            Link rootLink = Link.of("/", "root")
                .withTitle("Return to API root");

            // Add links in priority order: me first (primary next action), then others
            response.add(meLink);
            response.add(selfLink);
            response.add(aboutLink);
            response.add(rootLink);

            return ResponseEntity
                .created(URI.create("/session"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(response);

        } catch (ObpAuthenticationException e) {
            log.error("Authentication failed for user {}: {}", credentials.username(), e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(null);
        } catch (ObpClientException e) {
            log.error("OBP service error during authentication for user {}: {}", credentials.username(), e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<SessionStatusResponse> getSession(
        HttpServletRequest request) {

        // This endpoint is protected and optional for Phase 1
        // Returns session metadata

        SessionStatusResponse response = new SessionStatusResponse("Bearer");

        Link selfLink = linkTo(methodOn(SessionController.class).getSession(null)).withSelfRel()
            .andAffordance(afford(methodOn(SessionController.class).deleteSession(null)));

        Link aboutLink = Link.of("/docs/session", "about")
            .withType("text/markdown")
            .withTitle("Session semantics (MUST READ)");

        Link meLink = Link.of("/users/me", "me");
        Link rootLink = Link.of("/", "root");

        response.add(selfLink);
        response.add(aboutLink);
        response.add(meLink);
        response.add(rootLink);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteSession(HttpServletRequest request) {
        // Extract Bearer token and revoke it
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            tokenStore.revoke(token);
        }

        return ResponseEntity.noContent().build();
    }

    // Inner class for GET /session response
    public static class SessionStatusResponse extends RepresentationModel<SessionStatusResponse> {
        private final String tokenType;

        public SessionStatusResponse(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getTokenType() {
            return tokenType;
        }
    }
}
