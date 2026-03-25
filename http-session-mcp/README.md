# http-session-mcp

Streamable MCP server for a thin authenticated HTTP gateway.

## Current Scope

This module currently implements minimal `http_get` and `http_post` tools backed by Spring `RestClient`.
The initial version intentionally keeps the behavior small:

- validates absolute `http` and `https` URLs
- performs GET and POST requests
- forwards caller headers except `Authorization`, `Cookie`, and `Host`
- returns only `status`, reduced `headers`, and `body`
- maps JSON-like responses to JSON objects
- maps other textual responses to strings

## Tool Surface

- `http_get`
- `http_post`

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
