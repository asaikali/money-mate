# Money Mate API - Implementation Plan

## Phase 1: Agent Protocol Foundation (Pure Handshake)

**Goal**: Establish the control plane for agent interaction - verify agents can connect, read instructions, and refuse to hallucinate URLs

**URL Structure**:
- `/` - Root endpoint (public, returns HAL response with handshake)
- `/AGENTS.md` - Agent instructions (follows agents.md convention)

**Root Response (`GET /`)**:
```json
{
  "agent_bootstrap": "Before taking any action, you MUST fetch and obey the resource referenced by _links.profile. Do not navigate or act until it has been read.",
  "_links": {
    "self": {
      "href": "http://localhost:8080/"
    },
    "profile": {
      "href": "http://localhost:8080/AGENTS.md",
      "type": "text/markdown",
      "title": "Agent Instructions - MUST READ"
    }
  }
}
```

**Agent Instructions** (`/AGENTS.md`):
Markdown document following agents.md convention with API-specific guidance:
- "This API follows HAL and HAL-FORMS hypermedia standards"
- "You MUST navigate exclusively by following relations exposed in _links"
- "You MUST NOT construct or infer URLs"
- "You MUST perform state-changing operations only via operations described in _templates"
- "If a required link or template is absent, the operation is not allowed"
- Current state: Unauthenticated (see root response for available operations)

**Tasks**:
1. Create HATEOAS response model (RepresentationModel class)
2. Create ApiRootController with `/` endpoint
3. Create AgentInstructionsController for `/AGENTS.md` endpoint (serves markdown)
4. Configure Spring HATEOAS for HAL+JSON content type
5. Test with curl and validate HAL structure

**Testing**:
- Manual curl testing
- Verify agent can self-bootstrap from `/`
- Verify `/AGENTS.md` is discoverable via profile link and readable
- Test refusal behavior: request an operation with no corresponding link (e.g., "list orders") - agent should refuse or fail gracefully, NOT hallucinate a URL

**Acceptance Criteria**:
- Unit test verifies root response structure
- Unit test checks `_links` contains `self` and `profile` with correct hrefs
- Unit test validates `profile` link points to `/AGENTS.md` with `type: "text/markdown"`
- Unit test verifies `agent_bootstrap` field is present with explicit imperative instruction
- Unit test confirms `/AGENTS.md` endpoint returns markdown content with agent instructions
- Integration test: verify agent reads `/AGENTS.md` before attempting any navigation
- Integration test: verify agent refuses to construct URLs when no links are present

---

## Phase 2: Add Spring Security

**Goal**: Protect API with Spring Security, session-based authentication

**URL Structure** (additions):
- `/login` - Login endpoint (accepts username/password)

**Root Response** (`GET /`) - Authenticated variant adds:
```json
{
  "_links": {
    "login": { "href": "/login" }
  },
  "_templates": {
    "default": {
      "title": "Login to Money Mate API",
      "method": "POST",
      "target": "/login",
      "properties": [
        { "name": "username", "required": true },
        { "name": "password", "required": true, "type": "password" }
      ]
    }
  },
  "authenticated": false
}
```

**Tasks**:
1. Add spring-boot-starter-security dependency
2. Configure SecurityFilterChain
3. Make `/`, `/AGENTS.md` public (no authentication required)
4. Implement `/login` endpoint
5. Store authentication in HTTP session
6. Update root response to reflect authenticated state
7. Test authentication flow with curl

**Testing**:
- Verify unauthenticated access to root and AGENTS.md
- Verify login endpoint accepts credentials
- Verify session persistence
- Verify authenticated root response shows authenticated: true

---

## Phase 3: Add Protected User Resource

**Goal**: Create authenticated user endpoint with OBP integration

**URL Structure** (additions):
- `/user` - Current user profile (protected, requires authentication)

**Root Response** (`GET /`) - Authenticated variant adds:
```json
{
  "_links": {
    "user": {
      "href": "/user",
      "title": "Current User Profile"
    },
    "logout": { "href": "/logout" }
  },
  "user": {
    "name": "John Doe",
    "userId": "user-123"
  }
}
```

**User Resource** (`GET /user`):
```json
{
  "_links": {
    "self": { "href": "/user" },
    "profile": {
      "href": "/AGENTS.md",
      "type": "text/markdown"
    }
  },
  "userId": "user-123",
  "email": "john.doe@example.com",
  "username": "jdoe",
  "provider": "obp",
  "providerId": "user.provider.id"
}
```

**Tasks**:
1. Create UserController for `/user` endpoint
2. Protect `/user` with Spring Security
3. Integrate with ObpUserService to fetch user details
4. Add user link to authenticated root response
5. Update `/AGENTS.md` to include authenticated operation guidance
6. Implement logout endpoint
7. Test protected resource access

**Testing**:
- Verify unauthenticated requests to `/user` are rejected
- Verify authenticated requests return user details from OBP
- Verify logout clears session
- Verify `/AGENTS.md` content includes guidance for authenticated operations
