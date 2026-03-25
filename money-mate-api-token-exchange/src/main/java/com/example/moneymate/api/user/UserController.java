package com.example.moneymate.api.user;

import com.example.moneymate.api.obp.client.ObpClient;
import com.example.moneymate.api.obp.client.ObpClientException;
import com.example.moneymate.api.obp.client.UserDetailsResponse;
import com.example.moneymate.api.security.BrokeredObpTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final ObpClient obpClient;
    private final BrokeredObpTokenService brokeredObpTokenService;

    public UserController(ObpClient obpClient, BrokeredObpTokenService brokeredObpTokenService) {
        this.obpClient = obpClient;
        this.brokeredObpTokenService = brokeredObpTokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        try {
            String obpToken = brokeredObpTokenService.currentObpToken();

            UserDetailsResponse obpUser = obpClient.getCurrentUser(obpToken);

            var accounts = obpClient.getAccounts(obpToken);
            int accountCount = accounts.accounts().size();

            // Count unique banks
            long bankCount = accounts.accounts().stream()
                .map(account -> account.bankId())
                .distinct()
                .count();

            // Map OBP response to our UserResponse
            UserResponse response = new UserResponse(
                obpUser.username(),
                obpUser.email(),
                accountCount,
                (int) bankCount
            );

            Link selfLink = linkTo(methodOn(UserController.class).getCurrentUser()).withSelfRel();
            Link rootLink = Link.of("/", "root");
            Link accountsLink = Link.of("/accounts", "accounts").withTitle("All my accounts");
            Link banksLink = Link.of("/banks", "banks").withTitle("Banks I bank with");

            response.add(selfLink);
            response.add(rootLink);
            response.add(accountsLink);
            response.add(banksLink);

            return ResponseEntity.ok(response);

        } catch (ObpClientException e) {
            log.error("Failed to fetch user details from OBP: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
        } catch (IllegalStateException e) {
            log.error("Brokered OBP access is not available: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
        } catch (Exception e) {
            log.error("Unexpected error fetching user details: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
}
