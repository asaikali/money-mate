package com.example.moneymate;

import com.example.moneymate.obp.client.ObpAuthenticationService;
import com.example.moneymate.obp.client.ObpUserService;
import com.example.moneymate.obp.dto.user.UserDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ObpTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ObpTestRunner.class);

    private final ObpAuthenticationService authService;
    private final ObpUserService userService;

    public ObpTestRunner(ObpAuthenticationService authService, ObpUserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Testing OBP API Integration ===");

        try {
            // Test 1: DirectLogin Authentication
            log.info("Testing DirectLogin authentication...");
            String token = authService.authenticate();
            log.info("✓ Successfully authenticated. Token: {}...", token.substring(0, Math.min(20, token.length())));

            // Test 2: Get Current User
            log.info("Fetching current user details...");
            UserDetailsResponse user = userService.getCurrentUser();
            log.info("✓ Successfully retrieved user details:");
            log.info("  - User ID: {}", user.userId());
            log.info("  - Email: {}", user.email());
            log.info("  - Username: {}", user.username());
            log.info("  - Provider: {}", user.provider());
            log.info("  - Provider ID: {}", user.providerId());

            log.info("=== All tests passed! ===");

        } catch (Exception e) {
            log.error("❌ Test failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
