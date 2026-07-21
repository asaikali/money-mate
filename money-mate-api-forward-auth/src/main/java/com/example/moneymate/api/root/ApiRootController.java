package com.example.moneymate.api.root;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Root API controller providing the API entry point and HAL-FORMS profile.
 */
@RestController
public class ApiRootController {

    private static final String AGENTS_MD = """
        # AGENTS.md — Money Mate HAL-FORMS Profile

        This profile describes the additional HAL-FORMS conventions used by the
        Money Mate API. It documents representation semantics for clients; the
        caller continues to determine the goal of each interaction.

        ## Navigation

        - `_links.self` identifies the current resource.
        - Other entries in `_links` advertise resources reachable from the current
          representation.
        - Following advertised links lets clients navigate without constructing
          endpoint URLs from prior knowledge.
        - A missing link relation means that the relation is not advertised by the
          current representation.

        ## State transitions

        - `_templates` describes state transitions currently offered by a resource.
        - Each template supplies the HTTP `method`, target URL, and accepted input
          `properties` for that transition.
        - A missing template means that the transition is not currently advertised.
        - The server remains responsible for authentication, authorization, and
          request validation.

        ## Typical client flow

        1. Retrieve the API root.
        2. Select links relevant to the caller's goal.
        3. Follow links to discover related resources.
        4. Use a matching template when the caller requests a state change.
        5. Report API errors or capabilities that are not currently advertised.
        """;

    @GetMapping(value = "/", produces = {
        "application/prs.hal-forms+json",
        "application/hal+json"
    })
    public ApiRootResponse getRoot() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal());

        ApiRootResponse response = new ApiRootResponse();

        response.add(linkTo(methodOn(ApiRootController.class).getRoot()).withSelfRel());

        response.add(
            Link.of("/AGENTS.md")
                .withRel(LinkRelation.of("profile"))
                .withType("text/markdown")
                .withTitle("Money Mate HAL-FORMS Profile")
        );

        response.add(
            Link.of("/docs/api")
                .withRel(LinkRelation.of("about"))
                .withType("text/markdown")
                .withTitle("Money Mate API forwarded access overview")
        );

        if (isAuthenticated) {
            response.add(Link.of("/users/me", "me")
                .withTitle("Your user profile and available actions"));
            response.add(Link.of("/accounts", "accounts")
                .withTitle("All my accounts"));
        }

        return response;
    }

    @GetMapping(value = "/AGENTS.md", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String agentsMd() {
        return AGENTS_MD;
    }

    private static final String API_OVERVIEW = """
     # Money Mate API
     ## Purpose
    
     Money Mate is a personal finance aggregation API. This forwarded-auth variant
     expects a Bearer token on inbound requests and passes that token unchanged to a
     downstream egress gateway, which is responsible for any token exchange needed
     before OBP is called.
    
     ## Core Resources
    
     - **User** \s
       Represents the authenticated user and acts as the entry point to
       user-specific data. Discover via the `me` link relation once authenticated.
    
     - **Accounts** \s
       Bank accounts associated with the authenticated user. Discover via an
       `accounts` link relation.
    
     - **Banks** \s
       Financial institutions where the user holds accounts. Discover via a `banks`
       link relation.
    
     - **Transactions** (planned) \s
       Transaction history associated with accounts. When available, discover via
       link relations exposed from account resources.
    
     ## Authentication

     Access to user-specific resources requires a Bearer access token on each
     protected request.

     - This API does not expose a username/password login operation.
     - Present the Bearer token on authenticated requests.
     - The API forwards that token to the configured egress gateway when it calls
       downstream banking resources.
     - The API does not validate the token contents itself; it only requires the
       header to be present before protected routes are used.

     ## Hypermedia profile
    
     Additional HAL-FORMS conventions used by this API are documented by the
     `profile` resource linked from the API root.
     """;

    @GetMapping(value = "/docs/api", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String apiOverview() {
        return API_OVERVIEW;
    }
}
