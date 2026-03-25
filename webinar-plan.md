# Webinar Plan

## Goal

Show two versions of the same core idea:

1. A generic agent can drive a HATEOAS API by following links and forms.
2. The same API style can be operationalized with brokered authentication and MCP, without putting upstream credentials into the agent chat loop.

## Demo Structure

### Demo 1: HATEOAS Baseline

Use [`money-mate-api`](/Users/adib/dev/asaikali/money-mate/money-mate-api) unchanged.

Story:
- The API is hypermedia-driven.
- The agent can use `curl`.
- The agent follows the API surface instead of hardcoding workflow.
- Direct username/password login is acceptable here because the point is HATEOAS, not polished auth.

### Demo 2: Brokered Variant

Use these modules:
- [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp)
- [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker)
- [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered)

Story:
- Goose authenticates through the identity broker.
- The MCP server forwards the user JWT.
- The brokered API accepts the JWT as a normal OAuth resource server.
- The brokered API performs OAuth 2.0 token exchange with the identity broker.
- The identity broker translates enterprise identity into upstream OBP sandbox access.
- Goose never sees OBP sandbox credentials.

## Architecture Decisions

### Keep the Original API

Do not change [`money-mate-api`](/Users/adib/dev/asaikali/money-mate/money-mate-api) for the webinar.

Reason:
- It is the clean baseline HATEOAS demo.
- It keeps the first half of the talk stable.

### New Module for the Brokered API

Use [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered) as a separate module.

Reason:
- Avoid overloading one app with two auth personalities.
- Keep the contrast between the demos explicit.
- Preserve the original API for attendees.

### Token Exchange Belongs in the Brokered API

Do not put token exchange in [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp).

Reason:
- The brokered API should remain a generic HTTP API.
- MCP should be only one client path.
- The adapter should depend on standard bearer tokens and token exchange, not a specific MCP gateway.

### Deliberate Demo Simplification

During token exchange, the identity broker may return the actual OBP DirectLogin token as the exchanged `access_token`.

Reason:
- This keeps the demo small.
- The important part is the contract shape: OAuth token exchange in, downstream access token out.
- This can be improved later without changing the story.

## Identity Story for the Webinar

The identity broker represents a corporate IdP.

The user does **not** type OBP sandbox credentials into Goose.

Instead:
- The identity broker authenticates demo users using normal in-memory Spring Security users.
- The identity broker has a configured mapping from those demo users to OBP sandbox credentials.
- During token exchange, the broker uses that mapping to perform OBP DirectLogin.

This keeps the runtime story clean:
- login authenticates the human
- token exchange acquires downstream access on behalf of the human

## Current Status

### Completed

- Created webinar branch: `webinar-2026-03-25`
- Secured [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp) with [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker)
- Added open DCR support in [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker) so Goose can register
- Verified Goose reaches auth flow through the MCP server
- Scaffolded [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered)
- Converted [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered) into a resource-server scaffold
- Removed copied direct-login `/session` flow from the brokered API
- Added a brokered token seam via [`BrokeredObpTokenService.java`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered/src/main/java/com/example/moneymate/api/security/BrokeredObpTokenService.java)
- Verified full reactor tests pass

### In Progress

- Implement OAuth 2.0 token exchange in [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker)
- Wire [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered) to call token exchange and obtain an OBP DirectLogin token

## Remaining Implementation Plan

### Phase 1: Identity Broker User Mapping

Add demo users to [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker):
- `katja`
- `timo`
- `ellie`

Map each to OBP sandbox credentials in broker config.

The user logs into the broker with those broker-side users, not with raw OBP email/password in Goose.

### Phase 2: OAuth Token Exchange in Identity Broker

Extend [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker) so the token endpoint supports OAuth 2.0 token exchange.

Planned exchange flow:
- `money-mate-api-brokered` authenticates as a confidential client
- sends incoming user JWT as `subject_token`
- requests access for downstream OBP access

Broker behavior:
- validate confidential client
- validate subject token
- resolve subject-to-OBP mapping
- perform or reuse OBP DirectLogin
- return the OBP DirectLogin token as the exchanged `access_token`

### Phase 3: Brokered API Token Exchange Client

Implement a token exchange client inside [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered):
- replace the placeholder in [`BrokeredObpTokenService.java`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered/src/main/java/com/example/moneymate/api/security/BrokeredObpTokenService.java)
- call the broker token endpoint
- send the inbound user JWT as the subject token
- receive exchanged access token
- use that token when calling OBP

### Phase 4: End-to-End Demo Validation

Validate this runtime flow:

1. Goose authenticates through [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp)
2. [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker) issues user JWT
3. [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp) forwards the JWT to [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered)
4. [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered) performs token exchange with [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker)
5. [`identity-broker`](/Users/adib/dev/asaikali/money-mate/identity-broker) performs OBP DirectLogin and returns exchanged token
6. [`money-mate-api-brokered`](/Users/adib/dev/asaikali/money-mate/money-mate-api-brokered) calls OBP
7. Goose navigates the brokered HATEOAS API

## Important Constraints

- Keep [`money-mate-api`](/Users/adib/dev/asaikali/money-mate/money-mate-api) unchanged for the first demo.
- Keep token exchange out of [`http-session-mcp`](/Users/adib/dev/asaikali/money-mate/http-session-mcp).
- Keep the HATEOAS adapter generic and not MCP-specific.
- Use OAuth token exchange as the contract between the brokered API and identity broker.
- Avoid putting OBP credentials or OBP tokens into user-facing chat flows.
