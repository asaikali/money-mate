# money-mate

Money Mate is a webinar/demo repo for exploring two ways AI agents can interact with enterprise APIs:

- `money-mate-api`
  A HATEOAS banking adapter that is easy to drive with a generic agent loop and `curl`.
- `money-mate-api-token-exchange`
  The same adapter idea, but secured through a brokered identity flow and used through `http-session-mcp`.

Supporting modules:

- `identity-broker`
  OAuth 2.0 / OIDC broker with device flow, open dynamic client registration, and demo token exchange.
- `http-session-mcp`
  A thin MCP HTTP gateway that authenticates the user, forwards the bearer token, and exposes simple HTTP tools.

## Demo 1

Use `money-mate-api` to show the HATEOAS concept directly.

- agent uses `curl`
- API advertises links and forms
- workflow stays server-driven

## Demo 2

Use `http-session-mcp` with `money-mate-api-token-exchange` to show the same idea with brokered authentication.

- Goose authenticates through `identity-broker`
- `http-session-mcp` forwards the user JWT
- `money-mate-api-token-exchange` performs OAuth token exchange
- `identity-broker` turns that into downstream OBP sandbox access

## Modules

- [money-mate-api](/Users/adib/dev/asaikali/money-mate/money-mate-api)
- [money-mate-api-token-exchange](/Users/adib/dev/asaikali/money-mate/money-mate-api-token-exchange)
- [http-session-mcp](/Users/adib/dev/asaikali/money-mate/http-session-mcp)
- [identity-broker](/Users/adib/dev/asaikali/money-mate/identity-broker)

## Default Ports

- `identity-broker`: `9000`
- `http-session-mcp`: `9091`
- `money-mate-api`: `8080`
- `money-mate-api-token-exchange`: `8083`

## Demo Users

These are the sandbox users configured in `identity-broker`:

- `katja.fi.29@example.com` / `ca0317`
- `timo.fi.29@example.com` / `6addcd`
- `ellie.de.29@example.com` / `2efb1f`

## Running

Start the broker and MCP server:

```bash
./mvnw spring-boot:run -q -pl identity-broker
./mvnw spring-boot:run -q -pl http-session-mcp
```

Start either API, depending on the demo:

```bash
./mvnw spring-boot:run -q -pl money-mate-api
./mvnw spring-boot:run -q -pl money-mate-api-token-exchange
```

## Verification

Run the reduced reactor tests:

```bash
./mvnw test
```
