# Security Roadmap

This document captures the phased plan to evolve Money Mate from local session auth into an agent-friendly OAuth/OIDC architecture.

## Locked Decisions

1. Auth model for protected APIs: OAuth 2.0 Resource Server with JWT access tokens.
2. Human login UX for agents: Device Authorization Flow.
3. Client ID: `money-mate-hypermedia-client`.
4. Scope: `hypermedia.access`.
5. Access token TTL: 15 minutes.
6. `money-mate-api` will stop using `/session`.
7. Phase 1 audience is `protected-test-api`.
8. For device flow bootstrap, `protected-test-api` provides hypermedia instructions, but agent calls `identity-broker` endpoints directly.
9. `protected-test-api` does not proxy or orchestrate device flow in Phase 1.
10. Phase transitions require explicit user approval before implementation continues.

## Phase Overview

1. Phase 1: Device Flow Proof with `identity-broker` + `protected-test-api`
2. Phase 2: Protected Resource Metadata (RFC 9728)
3. Phase 3: Integrate JWT auth into `money-mate-api`
4. Phase 4: Build `credential-broker`
5. Phase 5: Add token exchange in `identity-broker`
6. Phase 6: Wire `money-mate-api` to `credential-broker` and OBP
7. Phase 7: Security hardening and operations

## Phase 1 - Device Flow Proof

### Goal
Prove an AI agent can complete OAuth device flow end-to-end and access a protected HATEOAS endpoint without copy/pasting credentials into chat. Phase 1 focuses purely on device flow, not protected resource metadata.

### Build
1. New module: `identity-broker` (Spring Authorization Server).
2. New module: `protected-test-api` (Spring Boot API + HATEOAS + resource server).
3. `protected-test-api` root returns HAL-FORMS and includes a device-flow bootstrap template.
4. Root includes:
   - link to `identity-broker` discovery/metadata
   - HAL-FORMS template for starting device flow:
     - method: `POST`
     - target: broker `device_authorization_endpoint`
     - properties: `client_id=money-mate-hypermedia-client`, `scope=hypermedia.access`
5. Agent executes device-flow calls against `identity-broker` directly.
6. `GET /protected` requires JWT and returns diagnostic payload:
   - short success message
   - current timestamp (changes every call)
   - authenticated user context from token claims (safe subset)
7. Unauthorized access to `/protected` returns plain `401` and `WWW-Authenticate: Bearer` (no RFC 9728 signaling in this phase).

### JWT Validation Rules in `protected-test-api`
1. Valid signature (broker JWKS).
2. `iss` matches `identity-broker`.
3. `aud` contains `protected-test-api`.
4. `scope` contains `hypermedia.access`.
5. Token is unexpired.

### Out of Scope
1. RFC 9728 protected resource metadata.
2. `money-mate-api` integration.
3. Credential broker and token exchange.
4. Any custom `/session` login endpoint.

### Exit Criteria
1. Agent starts from API root.
2. Agent obtains device authorization data from broker.
3. Agent presents verification URL to human.
4. Human logs in via browser.
5. Agent obtains JWT and successfully calls `/protected`.
6. Agent can complete flow with manual retry (human says "done") even if it does not do background polling.
7. Agent does not request username/password in chat for API login.

## Phase 2 - Protected Resource Metadata (RFC 9728)

### Goal
Reduce manual hinting by letting agents discover auth requirements through standards-based metadata.

### Build
1. Add `/.well-known/oauth-protected-resource` to `protected-test-api`.
2. Return `401` with `WWW-Authenticate: Bearer` including protected resource metadata reference.
3. Keep root HAL-FORMS links aligned with discovery metadata.
4. Preserve Phase 1 device-flow bootstrap template and behavior.

### Exit Criteria
1. Agent can recover from `401` using metadata and complete auth with fewer hints than Phase 1.
2. Agent no longer depends on manual auth hints beyond normal API discovery.

## Phase 3 - Integrate JWT Auth into `money-mate-api`

### Goal
Make `money-mate-api` a JWT resource server and remove local session auth.

### Build
1. Remove `/session` auth model.
2. Configure JWT validation against `identity-broker`.
3. Enforce `iss`, `aud=money-mate-api`, and `scope=hypermedia.access`.
4. Keep HATEOAS behavior for authenticated vs unauthenticated flows.
5. Update `/AGENTS.md` and docs for device-flow-based authentication.

### Exit Criteria
1. Agent can authenticate and call existing protected Money Mate endpoints with broker-issued JWTs.

## Phase 4 - Build `credential-broker`

### Goal
Introduce a dedicated protected service that returns OBP credentials for an authenticated user identity.

### Build
1. Create `credential-broker` as OAuth-protected resource server.
2. Define API contract: input user identity, output OBP username/password.
3. Enforce access controls and audit logging on credential retrieval.

### Exit Criteria
1. `credential-broker` can securely return credentials for authorized identity requests.

## Phase 5 - Token Exchange in `identity-broker`

### Goal
Allow `money-mate-api` to exchange user token context for a downstream token with `aud=credential-broker`.

### Build
1. Implement token exchange flow in `identity-broker`.
2. Policy-check whether the user/client is allowed to exchange.
3. Issue exchanged token scoped for calling `credential-broker`.

### Exit Criteria
1. `money-mate-api` can obtain valid exchanged token for credential broker calls.

## Phase 6 - Wire `money-mate-api` -> `credential-broker` -> OBP

### Goal
Replace static OBP auth with runtime identity-driven credential retrieval.

### Build
1. `money-mate-api` extracts user identity from JWT.
2. `money-mate-api` performs token exchange with `identity-broker`.
3. `money-mate-api` calls `credential-broker` to fetch OBP credentials.
4. `money-mate-api` calls OBP using returned credentials.

### Exit Criteria
1. Real Money Mate flows succeed using exchanged downstream tokens and brokered credentials.

## Phase 7 - Hardening and Operations

### Goal
Prepare the architecture for reliability, observability, and security controls.

### Build
1. Audit logs and trace IDs across broker/API/broker chain.
2. Revocation and incident handling strategy.
3. Token/cache TTL tuning and retry/backoff policies.
4. Security and failure-mode test suites.
5. Operational runbooks and troubleshooting guides.

### Exit Criteria
1. End-to-end system has reproducible security behavior and operational playbooks.
