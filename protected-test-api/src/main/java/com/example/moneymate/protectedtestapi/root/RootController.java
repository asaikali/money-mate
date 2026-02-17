package com.example.moneymate.protectedtestapi.root;

import com.example.moneymate.protectedtestapi.config.IdentityBrokerProperties;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class RootController {

    private static final String CLIENT_ID = "protected-test-api-hypermedia-client";
    private static final String SCOPE = "hypermedia.access";
    private static final String AGENTS_MD = """
        # AGENTS.md — API Contract for AI Agents
        
        This API is HATEOAS-based and uses HAL / HAL-FORMS.
        Treat the current response as the source of truth for what is possible right now.
        
        ## Hypermedia Rules
        
        Use `_links` for navigation and `_templates` for actions.
        
        Do not guess or construct URLs.
        If a link relation is missing, that path is not available in the current state.
        If an action template is missing, that action is not permitted in the current state.
        
        Always follow the template exactly (`method`, `target`, `properties`).
        
        ## Security: OAuth2 Device Flow
        
        For protected endpoints, obtain an access token from the authorization server using OAuth2 Device Flow.
        
        1. Discover auth metadata and device-flow controls from the API response.
        2. Start device authorization using the provided template/endpoint.
        3. Present `verification_uri_complete` to the user and ask them to open it.
        4. If `verification_uri_complete` is not provided, present `verification_uri` and `user_code`.
        5. Ask the user to confirm when they have completed login/consent.
        6. Exchange the `device_code` at the token endpoint until an `access_token` is returned.
        7. Call protected endpoints with `Authorization: Bearer <access_token>`.
        
        Do not ask the user to provide username/password in chat.
        
        ## Contract Enforcement
        
        If the user asks for operations not exposed by current `_links`/`_templates`, refuse and explain that the request is outside the API contract.
        """;

    private final IdentityBrokerProperties identityBrokerProperties;

    public RootController(IdentityBrokerProperties identityBrokerProperties) {
        this.identityBrokerProperties = identityBrokerProperties;
    }

    @GetMapping(value = "/", produces = {
        "application/prs.hal-forms+json",
        "application/hal+json"
    })
    public RootResponse root() {
        RootResponse response = new RootResponse();
        response.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        response.add(Link.of("/AGENTS.md")
            .withRel(LinkRelation.of("profile"))
            .withType("text/markdown")
            .withTitle("Agent instructions for OAuth device flow"));
        response.add(Link.of("/protected")
            .withRel(LinkRelation.of("protected"))
            .withType(MediaType.APPLICATION_JSON_VALUE)
            .withTitle("Protected resource requiring bearer token"));
        response.add(Link.of(identityBrokerProperties.getMetadataUri())
            .withRel(LinkRelation.of("authorization-server-metadata"))
            .withType(MediaType.APPLICATION_JSON_VALUE)
            .withTitle("Identity Broker discovery metadata"));

        response.addTemplate("start-device-flow", new RootResponse.HalFormsTemplate(
            "Start OAuth device flow",
            "POST",
            identityBrokerProperties.getDeviceAuthorizationEndpoint(),
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            List.of(
                new RootResponse.HalFormsProperty("client_id", true, CLIENT_ID),
                new RootResponse.HalFormsProperty("scope", true, SCOPE)
            )
        ));

        return response;
    }

    @GetMapping(value = "/AGENTS.md", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String agentsMd() {
        return AGENTS_MD;
    }
}
