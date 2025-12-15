# Money Mate API - HATEOAS Design

## Design Philosophy

Money Mate API is a **hypermedia-driven REST API** designed to be **AI-agent-friendly** from the ground up. The API follows HAL (Hypertext Application Language) and HAL-FORMS standards, using hypermedia controls to guide agents through available operations without requiring prior knowledge of the API structure.

### Core Principle

**This is a hypermedia API that strictly follows HAL and HAL-FORMS and is intentionally designed to be AI-agent-friendly.**

The API defines all valid behavior through hypermedia controls:
- If a `profile` link is present, it contains authoritative instructions that agents MUST read and obey before taking any action
- If an `about` link is present, it contains essential contextual information that agents MUST read before planning or acting
- Agents MUST navigate exclusively by following relations exposed in `_links`
- Agents MUST NOT construct or infer URLs
- Agents MUST perform state-changing operations only via operations described in `_templates`
- If a required template is absent, the operation is not allowed

---

## Standards & Specifications

### HAL (Hypertext Application Language)
- **Media Type**: `application/hal+json`
- **Specification**: https://stateless.group/hal_specification.html
- **Purpose**: Provides `_links` for navigation and `_embedded` for related resources

### HAL-FORMS
- **Media Type**: `application/prs.hal-forms+json`
- **Purpose**: Extends HAL with `_templates` to describe available operations
- **Use Case**: Tells agents HOW to perform actions (method, target, required fields)

### Link Relations
- **`self`** - Canonical URL of current resource
- **`profile`** (RFC 6906) - Points to agent contract with processing rules and conventions
- **`about`** (RFC 6903) - Points to semantic context and API overview
- **Custom relations** - Defined per-resource (e.g., `login`, `accounts`, `user`)

---

## "Belt and Suspenders" Discoverability

To ensure agents can self-bootstrap with **zero prior knowledge**, we employ multiple redundant discovery mechanisms:

### Layer 1: In-Response Hint (Suspenders #1)
Every response includes a human-readable `message` field that hints at the presence of `profile` and `about` links.

```json
{
  "message": "Welcome to Money Mate API. This is a HAL+Forms hypermedia API designed for AI agents. Read the 'profile' and 'about' links before proceeding."
}
```

### Layer 2: Link Metadata (Suspenders #2)
Link relations include descriptive `title` attributes that signal importance.

```json
{
  "_links": {
    "profile": {
      "href": "/profiles/agent-contract",
      "type": "text/markdown",
      "title": "Agent Contract - Required Reading"
    }
  }
}
```

### Layer 3: Profile Link (Belt #1)
The `profile` link (RFC 6906) points to the **authoritative agent contract** - a document that defines:
- How the API works
- Required agent behavior
- Navigation and action rules
- Current context-specific guidance

### Layer 4: About Link (Belt #2)
The `about` link provides:
- API purpose and capabilities
- Semantic context
- Design philosophy
- High-level overview

**Why Multiple Layers?**

An agent that:
- ✅ **Knows HAL** → Will check for `profile` and `about` links (standard behavior)
- ✅ **Doesn't know HAL** → Will see the `message` field hint
- ✅ **Ignores everything** → Will still see link titles when exploring `_links`

---

## URL Structure

### Public Endpoints (No Authentication Required)
- **`/api`** - API root (returns different response based on authentication state)
- **`/profiles/agent-contract`** - Agent contract (context-specific)
- **`/about`** - API overview and context

### Authentication Endpoints
- **`/api/login`** - Login endpoint (POST username/password via HAL-FORMS template)

### Authenticated Endpoints
- **`/api/user`** - Current user profile
- **`/api/accounts`** - User's bank accounts
- **`/api/transactions`** - Transaction history
- *(Additional resources discovered via hypermedia)*

---

## Response Examples

### Unauthenticated Root (`GET /api`)

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8080/api"
    },
    "profile": {
      "href": "http://localhost:8080/profiles/agent-contract",
      "type": "text/markdown",
      "title": "Agent Contract - Required Reading"
    },
    "about": {
      "href": "http://localhost:8080/about",
      "type": "text/markdown",
      "title": "Money Mate API - Overview and Context"
    },
    "login": {
      "href": "http://localhost:8080/api/login"
    }
  },
  "_templates": {
    "default": {
      "title": "Login to Money Mate API",
      "method": "POST",
      "target": "http://localhost:8080/api/login",
      "properties": [
        {
          "name": "username",
          "required": true,
          "prompt": "OBP Username"
        },
        {
          "name": "password",
          "required": true,
          "prompt": "OBP Password",
          "type": "password"
        }
      ]
    }
  },
  "message": "Welcome to Money Mate API. This is a HAL+Forms hypermedia API designed for AI agents. Read the 'profile' and 'about' links before proceeding.",
  "authenticated": false
}
```

### Authenticated Root (`GET /api` with session)

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8080/api"
    },
    "profile": {
      "href": "http://localhost:8080/profiles/agent-contract/authenticated",
      "type": "text/markdown",
      "title": "Agent Contract - Authenticated Operations"
    },
    "about": {
      "href": "http://localhost:8080/about",
      "type": "text/markdown",
      "title": "Money Mate API - Overview and Context"
    },
    "user": {
      "href": "http://localhost:8080/api/user",
      "title": "Current User Profile"
    },
    "accounts": {
      "href": "http://localhost:8080/api/accounts",
      "title": "Bank Accounts"
    },
    "logout": {
      "href": "http://localhost:8080/api/logout"
    }
  },
  "message": "Welcome back, John Doe. Navigate via _links to explore your banking data.",
  "authenticated": true,
  "user": {
    "name": "John Doe",
    "userId": "user-123"
  }
}
```

### Agent Contract (`GET /profiles/agent-contract`)

```markdown
# Money Mate API - Agent Contract

**REQUIRED READING**: Read this before navigating or performing actions.

## How This API Works

This is a hypermedia API that strictly follows HAL and HAL-FORMS and is
intentionally designed to be AI-agent-friendly.

The API defines all valid behavior through hypermedia controls:
- If a `profile` link is present, it contains authoritative instructions that
  you MUST read and obey before taking any action
- If an `about` link is present, it contains essential contextual information
  that you MUST read before planning or acting
- You MUST navigate exclusively by following relations exposed in `_links`
- You MUST NOT construct or infer URLs
- You MUST perform state-changing operations only via operations described in
  `_templates`
- If a required template is absent, the operation is not allowed

## Current State: Unauthenticated

You are at the API root without authentication. To access banking operations,
use the `login` template to submit credentials.

### How to Login
1. Use the `default` template from the root response
2. POST to the target with required `username` and `password` fields
3. After successful login, return to `/api` to discover authenticated resources

## Navigation Rules
- Follow `_links` relations - never construct URLs
- Check for `profile` and `about` links in responses
- Use `_templates` to discover available actions
```

### About Document (`GET /about`)

```markdown
# About Money Mate API

## Purpose
Money Mate is a personal banking API that provides AI agents access to banking
operations through a hypermedia-driven interface.

## What You Can Do
- View account balances
- Review transaction history
- Transfer money between accounts
- Manage user profile

## Authentication
This API uses session-based authentication. Login with your Open Bank Project
credentials to access your banking data.

## Design Philosophy
This API follows HATEOAS (Hypermedia as the Engine of Application State)
principles:

- **Never hardcode URLs** - Always follow links from responses
- **Discover actions through hypermedia** - Available operations are exposed
  via `_templates`, not documentation
- **Self-documenting** - The API tells you what you can do at each step
- **Agent-friendly** - Designed for AI agents to navigate without prior knowledge

## Technology
- **HAL** (Hypertext Application Language) for links and embedded resources
- **HAL-FORMS** for describing available operations
- **Standard link relations** (RFC 6906, RFC 8288) for discoverability
```

---

## Context-Specific Profiles

The agent contract varies based on authentication state:

- **`/profiles/agent-contract`** - Unauthenticated context (how to login)
- **`/profiles/agent-contract/authenticated`** - Authenticated context (available operations)

This allows the API to provide context-aware guidance while maintaining a
consistent contract structure.

---

## Design Decisions & Rationale

### Why HAL+Forms instead of JSON:API or GraphQL?
- **Hypermedia-first**: HAL+Forms is designed for HATEOAS
- **Agent-friendly**: Templates tell agents exactly how to perform actions
- **Discoverable**: No need for API documentation or schemas
- **Standard**: Uses IANA-registered link relations

### Why `profile` link for agent contract?
- **RFC 6906 standard**: `profile` is designed for "additional semantics and
  processing rules"
- **Semantic fit**: Agent contract defines how to process the API
- **Discoverable**: Standard relation that agents should know to check
- **Versioned**: Can have different profiles for different contexts

### Why both `profile` and `about`?
- **Separation of concerns**:
  - `profile` = HOW to behave (rules, contract)
  - `about` = WHAT this is (context, semantics)
- **RFC compliance**: Each has a specific, standardized purpose
- **Flexibility**: Can update rules without changing context

### Why session-based auth instead of JWT?
- **Simplicity**: No token management complexity
- **Appropriate scope**: Single-user banking API
- **Future-proof**: Can migrate to token-based later without breaking hypermedia

---

## Agent Interaction Flow

### First-Time Agent (No Prior Knowledge)

1. **Agent**: `GET /api`
2. **API**: Returns root with `profile` and `about` links in `_links`, plus hint in `message`
3. **Agent**: Sees message hint, follows `profile` link
4. **Agent**: `GET /profiles/agent-contract`
5. **API**: Returns markdown contract explaining rules
6. **Agent**: Reads contract, understands it must follow `_links` and use `_templates`
7. **Agent**: Optionally follows `about` link for context
8. **Agent**: Returns to root, examines `_templates` for available actions
9. **Agent**: Uses `login` template to authenticate
10. **Agent**: Returns to root, now sees authenticated links (`user`, `accounts`, etc.)

### Experienced Agent (Knows the Pattern)

1. **Agent**: `GET /api`
2. **Agent**: Checks for `profile` link (standard behavior)
3. **Agent**: `GET /profiles/agent-contract` (validates rules haven't changed)
4. **Agent**: Navigates via `_links`, acts via `_templates`

---

## Implementation Phases

### Phase 2: Package-by-Feature Refactoring
Reorganize code by business feature (authentication, user, etc.) before adding HATEOAS

### Phase 3: HATEOAS Scaffolding (This Design)
- Implement HAL+Forms responses
- Create profile and about endpoints
- Build root endpoint with belt-and-suspenders discoverability
- **No security yet** - just structure

### Phase 4: Add Spring Security
- Protect authenticated endpoints
- Integrate session-based authentication
- Enforce access controls

---

## References

- **RFC 6906** - The 'profile' Link Relation Type
  https://www.rfc-editor.org/rfc/rfc6906.html

- **RFC 8288** - Web Linking
  https://www.rfc-editor.org/rfc/rfc8288.html

- **HAL Specification**
  https://stateless.group/hal_specification.html

- **Spring HATEOAS Documentation**
  https://docs.spring.io/spring-hateoas/docs/current/reference/html/

- **IANA Link Relations Registry**
  https://www.iana.org/assignments/link-relations/link-relations.xhtml
