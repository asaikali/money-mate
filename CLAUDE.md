# Money Mate

AI-agent-friendly HATEOAS API experiment using Spring Boot and Spring AI.

## Stack
- Java 25, Spring Boot 3.5.8, Spring HATEOAS (HAL+JSON)
- Multi-module Maven: `money-mate-api` + `money-mate-agent`

## Modules

**money-mate-api** - Spring Boot HATEOAS API serving HAL+JSON responses for AI agents. Integrates with Open Bank Project (OBP) for banking operations.

**money-mate-agent** - Spring AI MCP server that acts as an AI agent, discovering and navigating the API by reading `/AGENTS.md` and following hypermedia controls.

## Conventions
- **Package-by-feature** - Not by layer (e.g., `api/root/` contains controller + model + tests)
- **HATEOAS-first** - Navigate via `_links`, act via `_templates`, no URL construction
- **AGENTS.md** - Agent instructions at `/AGENTS.md` endpoint

## Run
```bash
./mvnw test                    # All tests
cd money-mate-api && ../mvnw spring-boot:run
```

Endpoints: `GET /` (HAL+JSON root), `GET /AGENTS.md` (agent contract)

## Current Status
✅ Root endpoint with AGENTS.md
⏭️ Next: Spring Security + session auth
