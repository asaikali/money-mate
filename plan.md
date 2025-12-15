# Money Mate - Implementation Plan

## Phase 1: Add Spring Security

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

## Phase 2: Add Protected User Resource

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
