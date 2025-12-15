# Money Mate

AI-agent-friendly HATEOAS API experiment using Spring Boot and Spring AI.

## Stack
- Java 25, Spring Boot 3.5.8, Spring AI 1.1.2
- Spring HATEOAS (HAL+JSON), Spring AI MCP
- Multi-module Maven: `money-mate-api` + `money-mate-agent`

## Modules

**money-mate-api** - Spring Boot HATEOAS API serving HAL+JSON responses for AI agents. Integrates with Open Bank Project (OBP) for banking operations.

**money-mate-agent** - Spring AI MCP server exposing the "money-talk" tool via streamable HTTP on port 9090. Currently returns hardcoded financial data. Accessible via Goose MCP client at `http://localhost:9090/mcp`.

## Conventions
- **Package-by-feature** - Not by layer (e.g., `api/root/` contains controller + model + tests)
- **HATEOAS-first** - Navigate via `_links`, act via `_templates`, no URL construction
- **AGENTS.md** - Agent instructions at `/AGENTS.md` endpoint

## Run
```bash
./mvnw test                                  # All tests
cd money-mate-api && ../mvnw spring-boot:run # Run API on port 8080
cd money-mate-agent && ../mvnw spring-boot:run # Run MCP server on port 9090
```

**API Endpoints**: `GET /` (HAL+JSON root), `GET /AGENTS.md` (agent contract)

**MCP Server**: `http://localhost:9090/mcp` - Connect with Goose MCP client

## Current Status
✅ HATEOAS API root endpoint with AGENTS.md
✅ MCP server with money-talk tool (hardcoded responses)
⏭️ Next: Spring Security + session auth for API
