# money-mate

Money Mate is a webinar/demo repo for exploring two ways AI agents can interact with enterprise APIs:

- `money-mate-api`
  A HATEOAS banking adapter that is easy to drive with a generic agent loop and `curl`.
- `money-mate-api-token-exchange`
  The same adapter idea, but secured through a brokered identity flow and used through `http-session-mcp`.
- `money-mate-api-forward-auth`
  A forwarded-bearer variant that sends the caller token to an egress gateway instead of handling token exchange in the API itself.

Supporting modules:

- `identity-broker`
  OAuth 2.0 / OIDC broker with device flow, open dynamic client registration, and demo token exchange.
- `http-session-mcp`
  A thin MCP HTTP gateway that authenticates the user, forwards the bearer token, and exposes simple HTTP tools.
- `envoy-egress-gateway`
  Local Envoy egress gateway plus a Go gRPC external auth service that exchanges bearer tokens and rewrites them to OBP DirectLogin headers.

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

## Demo 3

Use `http-session-mcp` with `money-mate-api-forward-auth` and `envoy-egress-gateway` to show Envoy acting as the egress gateway.

- Goose authenticates through `identity-broker`
- `http-session-mcp` forwards the user JWT
- `money-mate-api-forward-auth` forwards that bearer token to Envoy
- `envoy-egress-gateway` performs token exchange and rewrites the OBP auth header
- OBP receives `Authorization: DirectLogin token=...`

## Modules

- [money-mate-api](/Users/adib/dev/asaikali/money-mate/money-mate-api)
- [money-mate-api-token-exchange](/Users/adib/dev/asaikali/money-mate/money-mate-api-token-exchange)
- [money-mate-api-forward-auth](/Users/adib/dev/asaikali/money-mate/money-mate-api-forward-auth)
- [http-session-mcp](/Users/adib/dev/asaikali/money-mate/http-session-mcp)
- [identity-broker](/Users/adib/dev/asaikali/money-mate/identity-broker)
- [envoy-egress-gateway](/Users/adib/dev/asaikali/money-mate/envoy-egress-gateway)

## Default Ports

- `identity-broker`: `9000`
- `http-session-mcp`: `9091`
- `money-mate-api`: `8080`
- `money-mate-api-token-exchange`: `8083`
- `money-mate-api-forward-auth`: `8084`
- `envoy-egress-gateway` auth service: `10003`
- `envoy-egress-gateway` Envoy listener: `10000`

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
./mvnw spring-boot:run -q -pl money-mate-api-forward-auth
```

For the Envoy egress demo, also start the Go auth service and Envoy:

```bash
./envoy-egress-gateway/run-envoy.sh
./envoy-egress-gateway/run-authz.sh
```

## Verification

Run the reduced reactor tests:

```bash
./mvnw test
```
