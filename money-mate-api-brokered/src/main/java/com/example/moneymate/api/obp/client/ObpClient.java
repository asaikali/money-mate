package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ObpClient {

    private static final Logger log = LoggerFactory.getLogger(ObpClient.class);

    private final RestClient publicRestClient;
    private final String consumerKey;
    private final String apiVersion;

    public ObpClient(
        @Qualifier("obpPublicRestClient") RestClient publicRestClient,
        ObpProperties properties
    ) {
        this.publicRestClient = publicRestClient;
        this.consumerKey = properties.auth().consumerKey();
        this.apiVersion = properties.api().version();
    }

    /**
     * Authenticate user with OBP DirectLogin and return OBP token.
     *
     * @param username User's email/username
     * @param password User's password
     * @return OBP DirectLogin token
     * @throws ObpAuthenticationException if authentication fails
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public String login(String username, String password) {
        String directLoginHeader = String.format(
            "username=%s, password=%s, consumer_key=%s",
            username, password, consumerKey
        );

        log.debug("Attempting OBP DirectLogin for user: {}", username);

        try {
            DirectLoginResponse response = publicRestClient.post()
                .uri("/my/logins/direct")
                .header("directlogin", directLoginHeader)
                .body("{}")
                .retrieve()
                .body(DirectLoginResponse.class);

            if (response == null || response.token() == null) {
                log.error("OBP DirectLogin returned null response or token for user: {}", username);
                throw new ObpAuthenticationException("OBP authentication failed: no token received");
            }

            log.info("Successfully authenticated user: {}", username);
            return response.token();

        } catch (RestClientException e) {
            log.error("OBP DirectLogin failed for user {}: {}", username, e.getMessage(), e);
            throw new ObpAuthenticationException("OBP authentication failed", e);
        }
    }

    /**
     * Get current user details from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @return User details from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public UserDetailsResponse getCurrentUser(String obpToken) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/users/current";

        log.debug("Fetching current user from OBP");

        try {
            UserDetailsResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(UserDetailsResponse.class);

            if (response == null) {
                log.error("OBP users/current returned null response");
                throw new ObpClientException("Failed to fetch user details from OBP");
            }

            log.debug("Successfully fetched user details for: {}", response.username());
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch user details from OBP: {}", e.getMessage(), e);
            throw new ObpClientException("Failed to fetch user details from OBP", e);
        }
    }

    /**
     * Get user's accounts from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @return Accounts from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public ObpAccountsResponse getAccounts(String obpToken) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/my/accounts";

        log.debug("Fetching accounts from OBP");

        try {
            ObpAccountsResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(ObpAccountsResponse.class);

            if (response == null) {
                log.error("OBP my/accounts returned null response");
                throw new ObpClientException("Failed to fetch accounts from OBP");
            }

            log.debug("Successfully fetched {} accounts", response.accounts().size());
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch accounts from OBP: {}", e.getMessage(), e);
            throw new ObpClientException("Failed to fetch accounts from OBP", e);
        }
    }

    /**
     * Get all banks from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @return Banks from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public ObpBanksResponse getBanks(String obpToken) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/banks";

        log.debug("Fetching banks from OBP");

        try {
            ObpBanksResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(ObpBanksResponse.class);

            if (response == null) {
                log.error("OBP banks returned null response");
                throw new ObpClientException("Failed to fetch banks from OBP");
            }

            log.debug("Successfully fetched {} banks", response.banks().size());
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch banks from OBP: {}", e.getMessage(), e);
            throw new ObpClientException("Failed to fetch banks from OBP", e);
        }
    }

    /**
     * Get account details including balance from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @param bankId Bank ID
     * @param accountId Account ID
     * @return Account details from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public ObpAccountDetailsResponse getAccountDetails(String obpToken, String bankId, String accountId) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/banks/" + bankId + "/accounts/" + accountId + "/owner/account";

        log.debug("Fetching account details for {}/{}", bankId, accountId);

        try {
            ObpAccountDetailsResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(ObpAccountDetailsResponse.class);

            if (response == null) {
                log.error("OBP account details returned null response for {}/{}", bankId, accountId);
                throw new ObpClientException("Failed to fetch account details from OBP");
            }

            log.debug("Successfully fetched account details for {}/{}", bankId, accountId);
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch account details from OBP for {}/{}: {}", bankId, accountId, e.getMessage(), e);
            throw new ObpClientException("Failed to fetch account details from OBP", e);
        }
    }

    /**
     * Get account transactions from OBP using DirectLogin token.
     *
     * @param obpToken OBP DirectLogin token
     * @param bankId Bank ID
     * @param accountId Account ID
     * @return Transactions from OBP
     * @throws ObpClientException if OBP is unreachable or returns error
     */
    public ObpTransactionsResponse getTransactions(String obpToken, String bankId, String accountId) {
        String directLoginHeader = "token=" + obpToken;
        String uri = "/obp/" + apiVersion + "/banks/" + bankId + "/accounts/" + accountId + "/owner/transactions";

        log.debug("Fetching transactions for {}/{}", bankId, accountId);

        try {
            ObpTransactionsResponse response = publicRestClient.get()
                .uri(uri)
                .header("directlogin", directLoginHeader)
                .retrieve()
                .body(ObpTransactionsResponse.class);

            if (response == null) {
                log.error("OBP transactions returned null response for {}/{}", bankId, accountId);
                throw new ObpClientException("Failed to fetch transactions from OBP");
            }

            log.debug("Successfully fetched {} transactions for {}/{}",
                response.transactions().size(), bankId, accountId);
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch transactions from OBP for {}/{}: {}", bankId, accountId, e.getMessage(), e);
            throw new ObpClientException("Failed to fetch transactions from OBP", e);
        }
    }
}
