package com.example.moneymate.api.transaction;

import com.example.moneymate.api.obp.client.ObpClient;
import com.example.moneymate.api.obp.client.ObpClientException;
import com.example.moneymate.api.obp.client.ObpTransactionsResponse;
import com.example.moneymate.api.security.SessionPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts/{accountId}/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final ObpClient obpClient;

    public TransactionController(ObpClient obpClient) {
        this.obpClient = obpClient;
    }

    @GetMapping
    public ResponseEntity<TransactionCollectionResponse> getTransactions(@PathVariable String accountId) {
        try {
            // Get SessionPrincipal from SecurityContextHolder
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SessionPrincipal principal = (SessionPrincipal) authentication.getPrincipal();

            // Fetch accounts to find bankId for this accountId
            var accountsResponse = obpClient.getAccounts(principal.obpToken());
            var account = accountsResponse.accounts().stream()
                .filter(acc -> acc.id().equals(accountId))
                .findFirst()
                .orElse(null);

            if (account == null) {
                log.warn("Account {} not found for user {}", accountId, principal.subject());
                return ResponseEntity.notFound().build();
            }

            String bankId = account.bankId();

            // Fetch transactions from OBP
            ObpTransactionsResponse transactionsResponse = obpClient.getTransactions(
                principal.obpToken(),
                bankId,
                accountId
            );

            // Map OBP transactions to TransactionResponse (without id field)
            List<TransactionResponse> transactions = transactionsResponse.transactions().stream()
                .map(obpTxn -> {
                    TransactionResponse txn = new TransactionResponse(
                        obpTxn.details().posted(),
                        obpTxn.details().description(),
                        obpTxn.details().value().amount(),
                        obpTxn.details().value().currency(),
                        obpTxn.details().newBalance().amount()
                    );

                    // Add links - using transaction ID only in the URL, not as a field
                    txn.add(Link.of(
                        "/accounts/" + accountId + "/transactions/" + obpTxn.id(),
                        "self"
                    ).withTitle("Transaction details"));

                    txn.add(Link.of(
                        "/accounts/" + accountId,
                        "account"
                    ).withTitle("Account"));

                    return txn;
                })
                .collect(Collectors.toList());

            // Build collection response (without accountId field)
            TransactionCollectionResponse response = new TransactionCollectionResponse(
                transactions.size(),
                transactions
            );

            // Add collection-level links
            response.add(Link.of(
                "/accounts/" + accountId + "/transactions",
                "self"
            ).withTitle("Account transactions"));

            response.add(Link.of(
                "/accounts/" + accountId,
                "account"
            ).withTitle("Back to account"));

            response.add(Link.of(
                "/",
                "root"
            ).withTitle("API root"));

            return ResponseEntity.ok(response);

        } catch (ObpClientException e) {
            log.error("Failed to fetch transactions from OBP: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
        } catch (Exception e) {
            log.error("Unexpected error fetching transactions: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
}
