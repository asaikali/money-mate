# Money Mate API Forward Auth

Spring Boot HATEOAS backend providing the forwarded-auth variant of the Money Mate API.

## Description

This module exposes the same style of HATEOAS banking adapter as `money-mate-api`, but expects an inbound bearer token and forwards it to an egress gateway instead of handling token exchange itself.

Current behavior:
- exposes the hypermedia API surface without the original `/session` login flow
- requires an `Authorization: Bearer ...` header on protected routes
- forwards the bearer token to the configured OBP egress gateway
- lets the egress gateway perform token exchange and DirectLogin header shaping

## Technology Stack

- Spring Boot 4.0.4
- Spring HATEOAS (HAL+Forms)
- Spring Web
- Spring Security
- Java 25

## Running

From the `money-mate-api-forward-auth` directory:

```bash
../mvnw spring-boot:run
```

## Configuration

See `src/main/resources/application.yaml` for configuration options.

The default configuration targets a local Envoy egress gateway at `http://localhost:10000`.

## Development Resources

- `../envoy-egress-gateway/` - local Envoy config and external auth service
- `../obp/http-client/` - shared HTTP request collection for testing OBP directly
