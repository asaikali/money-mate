package com.example.moneymate.identitybroker.security;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MutableInMemoryRegisteredClientRepository implements RegisteredClientRepository {

    private final Map<String, RegisteredClient> clientsById = new ConcurrentHashMap<>();
    private final Map<String, RegisteredClient> clientsByClientId = new ConcurrentHashMap<>();

    MutableInMemoryRegisteredClientRepository(RegisteredClient... registeredClients) {
        for (RegisteredClient registeredClient : registeredClients) {
            save(registeredClient);
        }
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        clientsById.put(registeredClient.getId(), registeredClient);
        clientsByClientId.put(registeredClient.getClientId(), registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return clientsById.get(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return clientsByClientId.get(clientId);
    }
}
