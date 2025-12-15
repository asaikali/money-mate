# Money Mate - Implementation Plan

## Phase 1: Implement Spring AI MCP Agent

**Goal**: Create an MCP (Model Context Protocol) server using Spring AI that exposes a stateless, streamable HTTP interface with one tool for answering money-related questions

**Architecture**:
- MCP server running on localhost (port 9090)
- Stateless HTTP transport for MCP protocol
- No security/authentication (Phase 1 only - will add later)
- Single tool: "money-talk" - conversational interface for discussing your finances, balances, transactions, and spending habits
- Connect via Goose MCP client for testing

**Tool Schema**:

Input:
```json
{
  "type": "object",
  "properties": {
    "message": {
      "type": "string",
      "description": "Your question or message about your finances"
    }
  },
  "required": ["message"]
}
```

Output:
```json
{
  "type": "object",
  "properties": {
    "response": {
      "type": "string",
      "description": "Conversational answer to your money question"
    }
  }
}
```

**Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

**Configuration** (`application.properties` or `application.yml`):
```yaml
server:
  port: 9090

spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE           # Required for streamable HTTP
        name: money-mate-agent
        version: 1.0.0
        type: SYNC                     # Synchronous server
        streamable-http:
          mcp-endpoint: /mcp           # Endpoint path (default)
        annotation-scanner:
          enabled: true                # Auto-detect @McpTool methods
```

**Tool Implementation**:
```java
package com.example.moneymate.agent.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MoneyTools {

    @McpTool(
        name = "money-talk",
        description = "Conversational interface for discussing your finances, balances, transactions, and spending habits"
    )
    public String moneyTalk(
        @McpToolParam(
            description = "Your question or message about your finances",
            required = true
        ) String message
    ) {
        // Phase 1: Hardcoded response
        return "Your current balance is $1,234.56. You spent $45.67 on coffee this month.";
    }
}
```

**Tasks**:
1. Add `spring-ai-starter-mcp-server-webmvc` dependency to `money-mate-agent/pom.xml`
2. Create `application.yml` with server port 9090 and MCP streamable HTTP configuration
3. Create `MoneyTools` class with `@McpTool` annotated `moneyTalk` method returning hardcoded response
4. Verify Spring Boot auto-configuration detects and registers the tool
5. Test server startup and MCP endpoint availability at `http://localhost:9090/mcp`
6. Connect with Goose MCP client using streamable HTTP transport
7. Verify Goose can discover the "money-talk" tool
8. Verify Goose can invoke the tool and receive the hardcoded response

**Testing**:
- Verify Spring Boot application starts successfully on port 9090
- Verify MCP endpoint is accessible at `http://localhost:9090/mcp`
- Verify annotation scanner auto-detects the `@McpTool` method
- Configure Goose MCP client to connect via streamable HTTP to `http://localhost:9090/mcp`
- Verify Goose discovers the "money-talk" tool in the tool list
- Invoke "money-talk" tool from Goose with a test message
- Verify hardcoded response is returned correctly
- Verify stateless operation (no session state maintained between requests)

**Acceptance Criteria**:
- MCP server exposes HTTP transport endpoint on port 9090 at `/mcp`
- Goose client can successfully connect via streamable HTTP
- Goose client can discover the "money-talk" tool
- Goose client can invoke the tool and receive hardcoded response
- Once working with Goose, we'll determine next steps

**Implementation Notes**:
- Spring AI MCP uses annotation-based tool registration via `@McpTool`
- Automatic JSON schema generation for tool parameters from `@McpToolParam`
- `spring.ai.mcp.server.protocol=STREAMABLE` is required for streamable HTTP transport
- Server type should be `SYNC` for synchronous operations (simpler than ASYNC)
- Annotation scanner is enabled by default - automatically discovers `@Component` classes with `@McpTool` methods
- MCP endpoint defaults to `/mcp` and can be customized via `spring.ai.mcp.server.streamable-http.mcp-endpoint`
- No manual tool callback registration needed - Spring Boot auto-configuration handles everything

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
