# http-session-mcp

Streamable MCP server scaffold for a thin authenticated HTTP gateway.

## Phase 1

This module currently exposes the v1 tool surface with placeholder behavior only.
The goal of this phase is to lock in:

- module structure
- Spring Boot and Spring AI MCP wiring
- tool names and DTOs
- configuration shape

The HTTP execution and downstream auth logic will be implemented in phase 2.

## Tool Surface

- `http_get`

## Contract Shape

Input:

```json
{
  "url": "http://localhost:8080/api/test",
  "headers": {
    "Accept": "application/json"
  }
}
```

Response:

```json
{
  "status": 200,
  "headers": {
    "content-type": "application/json"
  },
  "body": {
    "id": "123"
  }
}
```

In phase 1, `http_get` returns a placeholder `501` response instead of performing I/O.
