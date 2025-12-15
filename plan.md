# Money Mate - Implementation Plan

## Phase 1: Spring Security & Session Resource (Stubbed Auth)

**Goal**: Implement Spring Security with session lifecycle and complete HATEOAS flow using stubbed authentication. Verify the entire security architecture, token management, and hypermedia controls work correctly before adding OBP complexity.

**Why `/session` (not `/login`)**:
- Authentication is modeled as a **resource** (RESTful)
- `POST /session` = "create an authenticated session resource"
- `GET /session` = "query current session status"
- `DELETE /session` = "destroy session (logout)"
- Better hypermedia alignment than verb endpoints
- Logout is discoverable via `_templates`, not assumed

**URL Structure** (additions):
- `POST /session` - Create authenticated session (login)
- `GET /session` - Query session status (optional but useful)
- `DELETE /session` - Destroy session (logout)
- `GET /docs/session` - Session semantics documentation (markdown)
- `GET /users/me` - Current authenticated user profile and available actions

**Content Type**:
- Keep `application/hal+json` and include `_templates` (Spring HATEOAS supports this)

**HAL-FORMS Templates (How `_templates` Work)**:
Spring HATEOAS generates `_templates` automatically using **affordances**:

1. **Enable HAL-FORMS support** in configuration:
   ```java
   @Configuration
   @EnableHypermediaSupport(type = HypermediaType.HAL_FORMS)
   public class WebConfig {
   }
   ```

2. **Add affordances to links** in controllers:
   ```java
   // Root controller - unauthenticated state
   Link selfLink = linkTo(methodOn(ApiRootController.class).root()).withSelfRel();

   return EntityModel.of(rootData)
       .add(selfLink.andAffordance(afford(methodOn(SessionController.class).createSession(null))));
   ```

3. **Request body classes define template properties**:
   ```java
   class LoginRequest {
       @NotBlank
       private String username;

       @NotBlank
       private String password;
   }
   ```

   Spring HATEOAS inspects the `LoginRequest` class and generates:
   ```json
   "_templates": {
     "default": {
       "method": "POST",
       "target": "/session",
       "properties": [
         { "name": "username", "required": true },
         { "name": "password", "required": true }
       ]
     }
   }
   ```

4. **Template titles via rest-messages.properties**:
   ```properties
   # src/main/resources/rest-messages.properties
   _templates.default.title=Login (establish authenticated session)
   _templates.deleteSession.title=Logout (terminate session)

   # Property prompts (optional)
   username._prompt=Email address
   password._prompt=Password
   ```

**Key Pattern**: Don't manually build `_templates` - use affordances pointing to controller methods, and Spring HATEOAS generates templates from method signatures and JSR-303 annotations.

**Affordance Examples**:
```java
// In ApiRootController (unauthenticated)
Link selfLink = linkTo(methodOn(ApiRootController.class).root()).withSelfRel()
    .andAffordance(afford(methodOn(SessionController.class).createSession(null)));
// Generates "default" template for POST /session

// In SessionController POST response
Link sessionLink = linkTo(methodOn(SessionController.class).getSession()).withSelfRel()
    .andAffordance(afford(methodOn(SessionController.class).deleteSession()));
// Generates "deleteSession" template for DELETE /session

// In ApiRootController (authenticated)
Link selfLink = linkTo(methodOn(ApiRootController.class).root()).withSelfRel()
    .andAffordance(afford(methodOn(SessionController.class).deleteSession()));
// Generates logout template on root
```

**Security Model**:
- Root (`/`) is **public** - accessible without authentication
- Response adapts based on authentication state
- Unauthenticated: shows login template
- Authenticated: shows logout template and additional links

**Public Endpoints** (no authentication required):
- `GET /` - Root (adapts to authentication state)
- `GET /AGENTS.md` - Agent instructions (global rules)
- `GET /docs/session` - Session semantics documentation
- `POST /session` - Login (create session)

**Protected Endpoints** (Bearer token required):
- `GET /session` - Session status
- `DELETE /session` - Logout
- `GET /users/me` - User profile
- All future resources (accounts, transactions, etc.)

**Authentication Flow (Stubbed)**:
1. Client POSTs to `/session` with `{"username": "...", "password": "..."}`
2. **Server accepts any credentials** (or specific test credentials like "test@example.com" / "password")
3. **Server generates opaque token** (e.g., `MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234`)
4. Server stores mapping: `our_token → { username, createdAt }` (no OBP token yet)
5. Server returns our token to client with links to next resources
6. **Client uses our Bearer token**: `Authorization: Bearer MMAT-...`
7. Server intercepts our Bearer token, validates it exists in storage

**Token Design (Server-side)**:
- **Opaque tokens**: Random UUIDs prefixed with `MMAT-` (Money Mate Token)
- **Token storage**: ConcurrentHashMap<String, SessionData> (in-memory, simple)
- **Mapping for Phase 1**: `our_token → { username, createdAt }`
- **SessionData class**: Simple record/class with username and createdAt fields

**Spring Security Configuration**:
```java
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
                                          UuidBearerTokenAuthFilter bearerTokenFilter)
      throws Exception {

    http
      // Stateless API: no server-side HTTP session
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

      // Non-browser client: disable CSRF (only safe if you are not using cookies for auth)
      .csrf(csrf -> csrf.disable())

      // Add our Bearer token auth filter
      .addFilterBefore(bearerTokenFilter,
          UsernamePasswordAuthenticationFilter.class)

      // Authorization rules
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/", "/AGENTS.md", "/docs/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/session").permitAll()
        // logout requires auth (DELETE /session)
        .requestMatchers(HttpMethod.DELETE, "/session").authenticated()
        .anyRequest().authenticated()
      )

      // Return 401 (not redirect) when unauthenticated
      .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
      }));

    return http.build();
  }
}
```

**Bearer Token Authentication Filter**:
```java
@Component
public class UuidBearerTokenAuthFilter extends OncePerRequestFilter {

  private final SessionTokenStore tokenStore;

  public UuidBearerTokenAuthFilter(SessionTokenStore tokenStore) {
    this.tokenStore = tokenStore;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // If already authenticated, do nothing
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      String token = extractBearerToken(header);

      if (token != null) {
        tokenStore.find(token).ifPresent(session -> {
          // Create Authentication with SessionPrincipal
          var auth = new UsernamePasswordAuthenticationToken(
              session.principal(), null, session.authorities());
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(auth);
        });
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(String header) {
    if (header == null) return null;
    if (!header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) return null;
    String token = header.substring("Bearer ".length()).trim();
    return token.isEmpty() ? null : token;
  }
}
```

**Token Store Contract**:
```java
public interface SessionTokenStore {
  Optional<SessionPrincipal> find(String token);
  void revoke(String token);
  String create(String username); // Returns generated MMAT token
}

public record SessionPrincipal(String subject,
                               Collection<? extends GrantedAuthority> authorities) {
  public Object principal() { return subject; }
  public Collection<? extends GrantedAuthority> authorities() { return authorities; }
}
```

**Key Points**:
- Filter reads `Authorization: Bearer <token>` header
- Validates token via `SessionTokenStore.find(token)`
- Sets `UsernamePasswordAuthenticationToken` in SecurityContextHolder
- Principal is `SessionPrincipal` (subject = username, authorities empty for Phase 1)
- If token invalid/missing, filter does nothing (request stays unauthenticated)
- Exception handler returns 401 with `WWW-Authenticate: Bearer` header

**Package Structure** (organized by resource/domain):
- `com.example.moneymate.api.security` - Security infrastructure
  - SecurityConfig, UuidBearerTokenAuthFilter
  - SessionTokenStore (interface), InMemorySessionTokenStore (implementation)
  - SessionPrincipal (record)
- `com.example.moneymate.api.session` - Session resource
  - SessionController (handles POST /session, GET /session, DELETE /session, GET /docs/session)
  - LoginRequest, SessionResponse (DTOs)
- `com.example.moneymate.api.user` - User resource (Phase 1: stubbed)
  - UserController (handles GET /users/me)
  - UserResponse (DTO)
- `com.example.moneymate.api` - API root
  - ApiRootController (already exists, will be updated)

**Tasks**:
1. Add spring-boot-starter-security dependency to money-mate-api/pom.xml
2. Enable HAL-FORMS support:
   - Create WebConfig class with @EnableHypermediaSupport(type = HypermediaType.HAL_FORMS)
   - Create rest-messages.properties with template titles
3. Create request/response model classes:
   - LoginRequest (username, password fields with @NotBlank)
   - SessionResponse (token_type, access_token, authenticated fields)
   - UserResponse (id, username, provider fields)
4. Create SessionPrincipal record (subject, authorities fields)
5. Create SessionTokenStore interface:
   - `Optional<SessionPrincipal> find(String token)`
   - `void revoke(String token)`
   - `String create(String username)` - generates and stores MMAT token, returns token
6. Implement InMemorySessionTokenStore (implements SessionTokenStore):
   - Use ConcurrentHashMap<String, SessionPrincipal>
   - `create()` generates `MMAT-{UUID}` token using UUID.randomUUID()
   - Stores mapping: token → SessionPrincipal(username, empty authorities list)
7. Create SecurityConfig with SecurityFilterChain:
   - Public: GET /, /AGENTS.md, /docs/**, POST /session
   - Protected: DELETE /session, GET /session, GET /users/me, anyRequest
   - Stateless session management
   - CSRF disabled
   - Custom 401 entryPoint with WWW-Authenticate: Bearer
8. Create UuidBearerTokenAuthFilter (extends OncePerRequestFilter):
   - Extract Bearer token from Authorization header
   - Call tokenStore.find(token)
   - If found: create UsernamePasswordAuthenticationToken with SessionPrincipal and set in SecurityContextHolder
   - If not found: do nothing (request stays unauthenticated)
9. Implement `POST /session` endpoint (SessionController):
   - Accept LoginRequest with username/password (validate against test credentials or accept any)
   - Call tokenStore.create(username) to generate and store MMAT token
   - Return EntityModel<SessionResponse> with token and links
   - Add affordance for DELETE to generate logout template
10. Implement `GET /docs/session` endpoint in SessionController:
    - Controller method returning ResponseEntity<String> with multiline Java string
    - Headers: Content-Type: text/markdown, Cache-Control: public, max-age=3600
    - Part of SessionController (same class as other session endpoints)
11. Implement `GET /session` endpoint (optional but recommended):
    - Protected endpoint
    - Return session metadata from TokenStorageService
    - Return EntityModel with links and affordance for DELETE (logout template)
12. Implement `DELETE /session` endpoint:
    - Protected endpoint
    - Extract Bearer token from Authorization header
    - Call tokenStore.revoke(token) to invalidate token
    - Return 204 No Content
13. Implement `GET /users/me` endpoint in UserController:
    - Create UserController in api.user package
    - Protected endpoint (requires Bearer token)
    - Get username from SecurityContextHolder.getContext().getAuthentication().getPrincipal()
    - Return EntityModel<UserResponse> with stubbed data (id="me", username from principal, provider="stubbed")
    - Add HAL links (self, root)
    - No OBP integration yet
14. Update ApiRootController to adapt response based on authentication:
    - Check if SecurityContextHolder has authenticated user
    - Unauthenticated: Return EntityModel with `authenticated: false`, self + profile links
      - Add affordance to self link: .andAffordance(afford(methodOn(SessionController.class).createSession(null)))
      - This generates login template automatically
    - Authenticated: Return EntityModel with `authenticated: true`, self + profile + me + session links
      - Add affordance to self link: .andAffordance(afford(methodOn(SessionController.class).deleteSession()))
      - This generates logout template automatically
15. Test complete session lifecycle with curl using Bearer tokens

**Root Response** (`GET /`) - Unauthenticated (no Authorization header):
```json
{
  "agent_bootstrap": "Before taking any action, you MUST fetch and obey the resource referenced by _links.profile.",
  "authenticated": false,
  "_links": {
    "self": { "href": "/" },
    "profile": {
      "href": "/AGENTS.md",
      "type": "text/markdown",
      "title": "Agent Instructions - MUST READ"
    }
  },
  "_templates": {
    "default": {
      "title": "Login (establish authenticated session)",
      "method": "POST",
      "target": "/session",
      "properties": [
        { "name": "username", "required": true },
        { "name": "password", "required": true, "type": "password" }
      ]
    }
  }
}
```

**Note**: No `_links.session` when unauthenticated - the template is sufficient to discover how to create a session. Keep root minimal.

**Session Creation Endpoint** (`POST /session`):

Request:
```json
{
  "username": "test@example.com",
  "password": "password"
}
```

Success Response (201 Created):
```json
{
  "token_type": "Bearer",
  "access_token": "MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234",
  "authenticated": true,
  "_links": {
    "self": { "href": "/session" },
    "about": {
      "href": "/docs/session",
      "type": "text/markdown",
      "title": "Session semantics (MUST READ)"
    },
    "me": {
      "href": "/users/me",
      "title": "Your user profile and available actions"
    },
    "root": {
      "href": "/",
      "title": "Return to API root"
    }
  },
  "_templates": {
    "logout": {
      "title": "Logout (terminate session)",
      "method": "DELETE",
      "target": "/session",
      "properties": []
    }
  }
}
```

Headers:
- `Cache-Control: no-store, private`
- `Location: /session`

**Key Points**:
- Accept any credentials for Phase 1 (or hardcode test@example.com / password)
- `about` link points to `/docs/session` - detailed session semantics documentation
- Logout template included immediately - agent discovers how to logout without guessing
- Title clarifies intent: "Logout (terminate session)"

**Session About Endpoint** (`GET /docs/session`) - Public:

Returns markdown documentation explaining session semantics.

Response (200 OK):
```markdown
# Session semantics

This document explains what a **session** represents in this API and how an
agent must interact with it.

## What a session is
A session represents an **authenticated interaction state** between the client
and the API. When a session exists, requests may access protected resources
according to the links and templates exposed by the API.

A session is created **only** by executing the login operation exposed via a
HAL-FORMS template that targets `POST /session`.

## Access token usage
When a session is created, the API returns an opaque access token.

You MUST include this token on all subsequent authenticated requests using the
HTTP header:

```
Authorization: Bearer <access_token>
```

The access token has no meaning outside this API and MUST NOT be interpreted or
decoded by the client.

## Navigating after authentication
After creating a session, the API will expose links such as:

- `self` — the session resource
- `me` — the authenticated principal
- `root` — the API entrypoint

You MUST navigate using only the relations provided in `_links`.

## Logging out
A session is terminated **only** by executing the logout operation exposed via a
HAL-FORMS template on the session resource that targets `DELETE /session`.

If no logout template is present, logout is not available in the current state.

## Session expiration
If a session token is missing, invalid, or expired, the API will respond with
`401 Unauthorized`.

When this occurs, you MUST return to the API root and re-authenticate using the
hypermedia controls provided there.

## Authority
This document defines the semantics of the session resource.

At all times, the authoritative source of what actions are permitted is the
current API response, as expressed through `_links` and `_templates`.
```

Headers:
- `Content-Type: text/markdown;charset=utf-8`
- `Cache-Control: public, max-age=3600` (can be cached, changes rarely)

**Session Status Endpoint** (`GET /session`) - Protected, Optional:

Request Header:
```
Authorization: Bearer MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234
```

Success Response (200 OK):
```json
{
  "authenticated": true,
  "tokenType": "Bearer",
  "createdAt": "2025-12-15T06:47:45Z",
  "_links": {
    "self": { "href": "/session" },
    "about": {
      "href": "/docs/session",
      "type": "text/markdown",
      "title": "Session semantics (MUST READ)"
    },
    "me": { "href": "/users/me" },
    "root": { "href": "/" }
  },
  "_templates": {
    "logout": {
      "title": "Logout (terminate session)",
      "method": "DELETE",
      "target": "/session",
      "properties": []
    }
  }
}
```

**Logout Endpoint** (`DELETE /session`) - Protected:

Request Header:
```
Authorization: Bearer MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234
```

Success Response (204 No Content):
- Server invalidates token (removes from storage)
- No response body
- Subsequent requests with this token will return 401

Server Behavior:
1. Validate token exists
2. Remove token from TokenStorageService
3. Return 204

**Root Response** (`GET /`) - Authenticated (with `Authorization: Bearer MMAT-...`):

Request:
```http
GET / HTTP/1.1
Authorization: Bearer MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234
```

Response:
```json
{
  "agent_bootstrap": "Before taking any action, you MUST fetch and obey the resource referenced by _links.profile.",
  "authenticated": true,
  "_links": {
    "self": { "href": "/" },
    "profile": { "href": "/AGENTS.md", "type": "text/markdown", "title": "Agent Instructions - MUST READ" },
    "me": { "href": "/users/me", "title": "Your user profile and available actions" },
    "session": { "href": "/session", "title": "Current session" }
  },
  "_templates": {
    "logout": {
      "title": "Logout",
      "method": "DELETE",
      "target": "/session",
      "properties": []
    }
  }
}
```

**Key Differences**:
- `authenticated: true` (vs `false`)
- Additional links: `me`, `session` (not present when unauthenticated)
- Template changes from `default` (login) to `logout`
- Root remains public - authentication only changes the response content

**Current User Endpoint** (`GET /users/me`) - Protected, Stubbed:

Request Header:
```
Authorization: Bearer MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234
```

Success Response (200 OK):
```json
{
  "id": "me",
  "username": "test@example.com",
  "provider": "stubbed",
  "_links": {
    "self": { "href": "/users/me" },
    "root": { "href": "/" }
  }
}
```

**Note**: This is stubbed data. Username comes from the token in storage. No OBP integration yet. Phase 2 will add real OBP user data and additional links.

Error Response (401 Unauthorized) - Missing or invalid token:
```json
{
  "error": "Authentication required",
  "authenticated": false,
  "_links": {
    "root": { "href": "/" }
  }
}
```

**Testing**:
- Verify `GET /` is public (works without Bearer token)
- Verify unauthenticated `GET /` returns `authenticated: false` with login template
- Verify unauthenticated `GET /` only has `self` and `profile` links
- Verify unauthenticated access to `/AGENTS.md` works (returns multiline string from controller)
- Verify unauthenticated access to `/docs/session` works
- Verify `POST /session` is public (works without Bearer token)
- Verify `POST /session` accepts test credentials (test@example.com / password)
- Verify `POST /session` returns `201 Created` with opaque token (`MMAT-...`) and logout template
- Verify server stores token in ConcurrentHashMap
- Verify `GET /session` with valid token returns session info and logout template
- Verify `GET /session` with invalid token returns 401
- Verify `DELETE /session` with valid token invalidates token and returns 204
- Verify subsequent requests with invalidated token return 401
- Verify `GET /users/me` requires Bearer token
- Verify `GET /users/me` with valid token returns stubbed user profile with username
- Verify `GET /users/me` with invalid token returns 401
- Verify authenticated `GET /` (with Bearer token) returns `authenticated: true`
- Verify authenticated `GET /` includes `me` and `session` links (not present when unauthenticated)
- Verify authenticated `GET /` has logout template (not login template)
- Verify authenticated `GET /` still accessible (root remains public, response adapts)
- Test complete session lifecycle:
  1. `GET /` → discover login template
  2. `POST /session` with test credentials → get MMAT token + logout template
  3. `GET /docs/session` → read session semantics
  4. `GET /users/me` with Bearer token → get stubbed profile
  5. `GET /` with Bearer token → see authenticated state
  6. `DELETE /session` → logout
  7. `GET /users/me` with same token → 401 (token invalidated)

**Success Criteria**:
- Complete HATEOAS flow working end-to-end
- Spring Security properly configured with public/protected endpoints
- Bearer token authentication working
- Session lifecycle (create, query, destroy) fully functional
- Root adapts correctly based on authentication state
- All hypermedia controls (links, templates) present and correct
- Can test entire flow with curl without any OBP dependency

---

## Phase 2: OBP Integration

**Goal**: Replace stubbed authentication with real OBP DirectLogin integration. Connect to OBP for login and user profile operations.

**Authentication Flow (OBP)**:
1. Client POSTs to `/session` with `{"username": "...", "password": "..."}`
2. **Server calls OBP DirectLogin**: `POST {{obp_host}}/my/logins/direct` with DirectLogin Authorization header
3. OBP returns `{"token": "eyJhbGci..."}`
4. Server generates opaque MMAT token (same as Phase 1)
5. **Server stores mapping**: `our_token → { obp_token, username, createdAt }`
6. Server returns our token to client (same response as Phase 1)
7. **Client uses our Bearer token**: `Authorization: Bearer MMAT-...`
8. **Server intercepts our Bearer token, looks up OBP token, uses DirectLogin for upstream calls**

**OBP Integration (Server-side only - never exposed to client)**:
- Login: `POST {{obp_host}}/my/logins/direct` (unversioned, no `/obp/vX.X.X` prefix)
- Login Header: `Authorization: DirectLogin username={{user}},password={{pass}},consumer_key={{key}}`
- Login Response: `{"token": "eyJhbGciOiJIUzI1NiJ9..."}`
- Upstream calls: `Authorization: DirectLogin token={{obp_token}}` (server maps from our_token)
- Consumer key stored in application.yml (never from client)
- User profile: `GET {{obp_host}}/obp/v5.1.0/users/current` (or latest version)

**Client Perspective** (OBP completely abstracted):
- Login with username/password via `/session` (same as Phase 1)
- Receive opaque Bearer token (format: `MMAT-{UUID}`)
- Use `Authorization: Bearer MMAT-...` in all subsequent requests
- No knowledge of OBP, DirectLogin, or consumer keys
- API responses identical to Phase 1 (except real OBP data in /users/me)

**Tasks**:
1. Add Spring RestClient configuration for OBP integration
2. Configure OBP properties in application.yml (host, consumer key)
3. Update SessionData to include obpToken field
4. Create ObpClientService that handles DirectLogin authentication using RestClient:
   - Method: `login(username, password)` → returns OBP token
   - Method: `getCurrentUser(obpToken)` → returns OBP user data
5. Update `POST /session` endpoint:
   - Call ObpClientService.login() instead of accepting any credentials
   - Store OBP token in SessionData along with username
   - Handle OBP errors (unreachable, invalid credentials) and map to appropriate HTTP responses
6. Update BearerTokenAuthenticationFilter:
   - Store OBP token in authentication context (for use by downstream controllers)
7. Update `GET /users/me` endpoint:
   - Extract OBP token from authentication context
   - Call ObpClientService.getCurrentUser(obpToken)
   - Return upstream OBP response (add HAL links)
   - Handle OBP errors (token expired, unreachable) and return appropriate responses
8. Test complete session lifecycle with real OBP credentials

**Updated Current User Endpoint** (`GET /users/me`) - With OBP:

Request Header:
```
Authorization: Bearer MMAT-7f8a2c9b-1c4e-4d0f-9b3c-0f7d9e4b1234
```

Success Response (200 OK):
```json
{
  "id": "me",
  "userId": "user-123",
  "username": "katja.fi.29@example.com",
  "provider": "obp",
  "_links": {
    "self": { "href": "/users/me" },
    "accounts": { "href": "/accounts", "title": "Your bank accounts" },
    "transactions": { "href": "/transactions", "title": "Your recent transactions" },
    "root": { "href": "/" }
  }
}
```

**Note**: `accounts` and `transactions` links are placeholders for Phase 3. They won't work until Phase 3 is implemented.

**Error Handling**:
- OBP unreachable during `POST /session`: Return 503 Service Unavailable
- OBP returns invalid credentials: Map to 401 with standard error response
- OBP token expired during `/users/me`: Return 401, client must re-authenticate

**Testing**:
- Verify `POST /session` calls OBP DirectLogin with correct headers
- Verify `POST /session` stores OBP token in SessionData
- Verify `POST /session` returns 401 on invalid OBP credentials
- Verify `POST /session` returns 503 on OBP unreachable
- Verify `GET /users/me` calls OBP users/current with DirectLogin token header
- Verify `GET /users/me` returns real OBP user data
- Verify `GET /users/me` handles OBP errors appropriately
- Verify server correctly translates our Bearer token to OBP DirectLogin for upstream calls
- Test complete session lifecycle with real OBP credentials

---

## Phase 3: Accounts & Transactions Resources

**Goal**: Expose user's bank accounts and transactions via OBP integration

**URL Structure** (additions):
- `GET /accounts` - List user's bank accounts
- `GET /accounts/{accountId}` - Get specific account details
- `GET /accounts/{accountId}/transactions` - List transactions for an account

**Next phase details to be defined after Phase 2 is complete.**
