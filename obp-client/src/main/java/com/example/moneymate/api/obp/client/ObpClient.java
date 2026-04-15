package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public abstract class ObpClient {

    private static final Logger log = LoggerFactory.getLogger(ObpClient.class);

    protected final RestClient publicRestClient;
    protected final String apiVersion;

    protected ObpClient(RestClient publicRestClient, ObpProperties properties) {
        this.publicRestClient = publicRestClient;
        this.apiVersion = properties.api().version();
    }

    protected abstract void applyAuth(RestClient.RequestHeadersSpec<?> spec, String token);

    public UserDetailsResponse getCurrentUser(String token) {
        String uri = "/obp/" + apiVersion + "/users/current";
        log.debug("Fetching current user from OBP");

        try {
            var spec = publicRestClient.get().uri(uri);
            applyAuth(spec, token);
            UserDetailsResponse response = spec.retrieve().body(UserDetailsResponse.class);

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

    public ObpAccountsResponse getAccounts(String token) {
        String uri = "/obp/" + apiVersion + "/my/accounts";
        log.debug("Fetching accounts from OBP");

        try {
            var spec = publicRestClient.get().uri(uri);
            applyAuth(spec, token);
            ObpAccountsResponse response = spec.retrieve().body(ObpAccountsResponse.class);

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

    public ObpBanksResponse getBanks(String token) {
        String uri = "/obp/" + apiVersion + "/banks";
        log.debug("Fetching banks from OBP");

        try {
            var spec = publicRestClient.get().uri(uri);
            applyAuth(spec, token);
            ObpBanksResponse response = spec.retrieve().body(ObpBanksResponse.class);

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

    public ObpAccountDetailsResponse getAccountDetails(String token, String bankId, String accountId) {
        String uri = "/obp/" + apiVersion + "/banks/" + bankId + "/accounts/" + accountId + "/owner/account";
        log.debug("Fetching account details for {}/{}", bankId, accountId);

        try {
            var spec = publicRestClient.get().uri(uri);
            applyAuth(spec, token);
            ObpAccountDetailsResponse response = spec.retrieve().body(ObpAccountDetailsResponse.class);

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

    public ObpTransactionsResponse getTransactions(String token, String bankId, String accountId) {
        String uri = "/obp/" + apiVersion + "/banks/" + bankId + "/accounts/" + accountId + "/owner/transactions";
        log.debug("Fetching transactions for {}/{}", bankId, accountId);

        try {
            var spec = publicRestClient.get().uri(uri);
            applyAuth(spec, token);
            ObpTransactionsResponse response = spec.retrieve().body(ObpTransactionsResponse.class);

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
