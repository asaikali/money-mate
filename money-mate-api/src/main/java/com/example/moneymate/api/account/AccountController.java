package com.example.moneymate.api.account;

import com.example.moneymate.api.obp.client.ObpAccountsResponse;
import com.example.moneymate.api.obp.client.ObpBanksResponse;
import com.example.moneymate.api.obp.client.ObpClient;
import com.example.moneymate.api.obp.client.ObpClientException;
import com.example.moneymate.api.security.SessionPrincipal;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final ObpClient obpClient;

    public AccountController(ObpClient obpClient) {
        this.obpClient = obpClient;
    }

    @GetMapping
    public ResponseEntity<AccountCollectionResponse> getAccounts() {
        try {
            // Get SessionPrincipal from SecurityContextHolder
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SessionPrincipal principal = (SessionPrincipal) authentication.getPrincipal();

            // Fetch accounts and banks from OBP
            ObpAccountsResponse accountsResponse = obpClient.getAccounts(principal.obpToken());
            ObpBanksResponse banksResponse = obpClient.getBanks(principal.obpToken());

            // Build bank lookup map (bankId -> bankName)
            Map<String, String> bankNameMap = banksResponse.banks().stream()
                .collect(Collectors.toMap(
                    ObpBanksResponse.Bank::id,
                    ObpBanksResponse.Bank::shortName
                ));

            // Map OBP accounts to AccountResponse with links
            List<AccountResponse> accounts = accountsResponse.accounts().stream()
                .map(obpAccount -> {
                    // Find IBAN from account routings
                    String iban = obpAccount.accountRoutings().stream()
                        .filter(routing -> "IBAN".equalsIgnoreCase(routing.scheme()))
                        .map(ObpAccountsResponse.AccountRouting::address)
                        .findFirst()
                        .orElse(null);

                    // Resolve bank name
                    String bankName = bankNameMap.getOrDefault(obpAccount.bankId(), obpAccount.bankId());

                    // Fetch account details to get balance
                    String currency = null;
                    String amount = null;
                    try {
                        var accountDetails = obpClient.getAccountDetails(
                            principal.obpToken(),
                            obpAccount.bankId(),
                            obpAccount.id()
                        );
                        if (accountDetails.balance() != null) {
                            currency = accountDetails.balance().currency();
                            amount = accountDetails.balance().amount();
                        }
                    } catch (ObpClientException e) {
                        log.warn("Failed to fetch balance for account {}/{}: {}",
                            obpAccount.bankId(), obpAccount.id(), e.getMessage());
                        // Continue without balance - don't fail entire request
                    }

                    AccountResponse account = new AccountResponse(
                        obpAccount.id(),
                        obpAccount.accountType(),
                        obpAccount.bankId(),
                        bankName,
                        iban,
                        currency,
                        amount
                    );

                    // Add links for each account
                    account.add(Link.of("/accounts/" + obpAccount.id(), "self").withTitle("Account details"));
                    account.add(Link.of("/banks/" + obpAccount.bankId(), "bank").withTitle(bankName));
                    account.add(Link.of("/accounts/" + obpAccount.id() + "/transactions", "transactions").withTitle("Transactions"));
                    account.add(Link.of("/accounts/" + obpAccount.id() + "/balance", "balance").withTitle("Balance"));

                    return account;
                })
                .collect(Collectors.toList());

            // Build collection response
            AccountCollectionResponse response = new AccountCollectionResponse(
                accounts.size(),
                accounts
            );

            // Add collection-level links
            Link selfLink = linkTo(methodOn(AccountController.class).getAccounts()).withSelfRel();
            Link rootLink = Link.of("/", "root").withTitle("API root");
            Link meLink = Link.of("/users/me", "me").withTitle("My profile");

            response.add(selfLink);
            response.add(rootLink);
            response.add(meLink);

            return ResponseEntity.ok(response);

        } catch (ObpClientException e) {
            log.error("Failed to fetch accounts from OBP: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
        } catch (Exception e) {
            log.error("Unexpected error fetching accounts: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
}
