# Secure Credential Brokering for AI Agents

## The Problem: OAuth Authorization URL Forwarding

When an AI agent needs to access a third-party resource (e.g., Google Calendar) on behalf of a user, it initiates an OAuth authorization code flow. The authorization server returns an authorization URL that the user must visit to grant consent.

This creates a security gap: **the authorization URL is just a link**. If User A's agent generates the URL but User B clicks it and completes consent, the agent ends up with a token for User B's resource — stored under User A's identity. User A now has unauthorized access to User B's data.

This is a variant of the classic OAuth CSRF / session fixation problem, but unique to the agent context where the human-in-the-loop consent step creates an opportunity for the wrong human to complete it.

The same class of vulnerability exists in the **Device Authorization Grant** (RFC 8628). The `user_code` and `verification_uri` can be shared — intentionally or accidentally — and nothing in the protocol binds the initiating user to the consenting user.

## Architecture Overview

This design closes the security gap using two cooperating services and the RFC 8693 Token Exchange delegation pattern.

| Service | Role |
|---------|------|
| **Identity Broker** | Authorization server that issues access and ID tokens. Supports token exchange (RFC 8693) to produce delegation tokens that encode both the human subject and the acting service. |
| **Credential Broker** | Service responsible for retrieving and storing third-party OAuth tokens on behalf of users. Only accepts delegation tokens issued by the Identity Broker. |

The key insight is that the Credential Broker never trusts a bare service token or user-supplied identity. It requires a **delegation token** that cryptographically binds the human user's identity to the calling agent service — and it independently verifies the user's identity at the consent callback.

## Flow 1: Obtaining a Delegation Token

Before the agent can request third-party credentials, it must obtain a delegation token from the Identity Broker via token exchange. This token encodes *who* the human is and *which* service is acting on their behalf.

```mermaid
sequenceDiagram
    participant User as Human User
    participant App as Agent Application
    participant IB as Identity Broker

    User->>App: Authenticates (login / SSO)
    App->>IB: Token Exchange (RFC 8693)<br/>subject_token = user's token<br/>actor_token = service's token
    IB->>IB: Validate both tokens<br/>Build delegation claims
    IB-->>App: Delegation Token<br/>{ sub: "user-123", act: { sub: "agent-svc-456" } }
```

The resulting delegation token contains an `act` claim per RFC 8693:

```json
{
  "sub": "user-123",
  "act": {
    "sub": "agent-svc-456"
  },
  "iss": "https://identity-broker.example.com",
  "aud": "https://credential-broker.example.com",
  "exp": 1719500000
}
```

The agent cannot forge this token. It had to present a valid user token — one that the user obtained by authenticating directly with the Identity Broker — to receive it.

## Flow 2: Initiating Third-Party Authorization

When the agent needs access to a third-party resource, it calls the Credential Broker with its delegation token. The Credential Broker generates an authorization URL and records the expected user identity.

```mermaid
sequenceDiagram
    participant Agent as Agent Service
    participant CB as Credential Broker
    participant IB as Identity Broker

    Agent->>CB: Request third-party token<br/>Authorization: Bearer {delegation_token}
    CB->>IB: Validate delegation token
    IB-->>CB: Token valid<br/>sub=user-123, act=agent-svc-456
    CB->>CB: Record pending authorization<br/>session_id=xyz, expected_sub=user-123
    CB-->>Agent: Authorization URL + session_id<br/>(User must complete consent)
```

At this point the Credential Broker knows exactly which user should complete the consent flow. This is the critical state that enables verification later.

## Flow 3: User Consent and Session Binding

The user navigates to the authorization URL, consents at the third-party provider, and is redirected back to the Credential Broker's callback endpoint. Here is where the security binding happens.

```mermaid
sequenceDiagram
    participant User as Human User (Browser)
    participant 3P as Third-Party Provider<br/>(Google, GitHub, etc.)
    participant CB as Credential Broker
    participant IB as Identity Broker

    User->>3P: Navigate to authorization URL
    User->>3P: Grant consent
    3P-->>User: Redirect to Credential Broker callback<br/>?code=abc&session_id=xyz

    User->>CB: GET /callback?code=abc&session_id=xyz

    Note over CB: No login form is presented.<br/>The user must authenticate,<br/>not supply a username.

    CB->>IB: Redirect user to authenticate<br/>(or validate existing session cookie)
    User->>IB: Authenticate (SSO / session / passkey)
    IB-->>CB: ID Token: { sub: "user-123" }

    CB->>CB: Compare authenticated sub<br/>against expected_sub for session_id

    alt Identity matches
        CB->>3P: Exchange authorization code for token
        3P-->>CB: Third-party access token
        CB->>CB: Store token under user-123
        CB-->>User: Success — return to agent session
    else Identity does not match
        CB->>CB: Reject and log attempt
        CB-->>User: Authorization denied
    end
```

The Credential Broker **forces authentication** at the callback. It redirects the user to the Identity Broker (or checks an existing session cookie from the Identity Broker). The user cannot type in a username — their identity comes from the authentication protocol. This is the mechanism that prevents a different user from completing someone else's consent flow.

## Flow 4: Agent Retrieves the Token

Once consent is complete and verified, the agent can retrieve the third-party token using the same delegation token.

```mermaid
sequenceDiagram
    participant Agent as Agent Service
    participant CB as Credential Broker
    participant IB as Identity Broker

    Agent->>CB: Get third-party token<br/>Authorization: Bearer {delegation_token}
    CB->>IB: Validate delegation token
    IB-->>CB: Valid — sub=user-123, act=agent-svc-456
    CB->>CB: Lookup stored token for user-123
    CB-->>Agent: Third-party access token
```

The delegation token ensures the agent can only retrieve tokens belonging to the user encoded in its `sub` claim. Even if another agent or service intercepts the flow, they would need a delegation token with a matching `sub` to access the stored credential.

## How the Security Gap Is Closed

The authorization URL forwarding attack fails at two independent enforcement points:

### Enforcement Point 1: Callback Authentication

When User B clicks User A's authorization URL and completes consent at the third-party provider, the browser redirects to the Credential Broker's callback. The Credential Broker does not accept a username — it forces authentication via the Identity Broker. User B authenticates as User B. The Credential Broker compares this against the expected identity (User A) recorded when the flow was initiated, finds a mismatch, and rejects the request. **User B's third-party token is never stored.**

### Enforcement Point 2: Delegation Token Binding on Retrieval

Even if the callback verification were somehow bypassed and a token were stored under the wrong identity, the retrieval path provides a second layer of defense. The agent must present a delegation token to fetch credentials. The Credential Broker validates the token with the Identity Broker and uses the `sub` claim — not any user-supplied value — to scope the lookup. An agent acting on behalf of User A can only retrieve User A's tokens.

### Why This Is Stronger Than Application-Level Session Checks

Some platforms (such as AWS Bedrock AgentCore) address this problem by delegating user verification to the application's callback endpoint. The application must check its own session state and call a completion API if the user matches.

This design improves on that approach in several ways:

- **The Credential Broker is the enforcement point**, not the application. There is no reliance on the application correctly implementing session verification.
- **Identity is established cryptographically** via tokens issued by the Identity Broker, not by inspecting application cookies or session stores.
- **The delegation token creates an unbroken chain of trust**: Human authenticates → Identity Broker issues user token → Token exchange produces delegation token with both subjects → Credential Broker validates the delegation token. No step in this chain relies on user input or application-level state.
- **No username or identity field is ever accepted from the user at the callback.** Authentication is the only path to establishing identity, eliminating an entire class of input-based attacks.

## Summary

| Threat | Mitigation |
|--------|------------|
| User A's authorization URL is forwarded to User B | Callback forces authentication; User B's identity does not match expected identity → rejected |
| Agent service attempts to retrieve another user's token | Delegation token's `sub` claim scopes retrieval; cannot be forged without a valid user token |
| Malicious service impersonates the agent | Delegation token's `act` claim identifies the service; Identity Broker validates the actor token |
| Application fails to verify session at callback | Verification is performed by the Credential Broker itself, not delegated to the application |
| User types in a different username at callback | No username input is accepted; identity comes from authentication only |
