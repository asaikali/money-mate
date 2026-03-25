# Money Mate API Token Exchange

Spring Boot HATEOAS backend providing the brokered-identity variant of the Money Mate API.

## Description

This module exposes the same style of HATEOAS banking adapter as `money-mate-api`, but expects brokered bearer tokens instead of direct username/password login.

Current behavior:
- validates inbound JWTs as an OAuth 2.0 resource server
- exposes the hypermedia API surface without the original `/session` login flow
- performs OAuth 2.0 token exchange with `identity-broker`
- uses the exchanged token as downstream OBP sandbox access

## Technology Stack

- Spring Boot 4.0.4
- Spring HATEOAS (HAL+Forms)
- Spring Web
- Spring Security Resource Server
- Java 25

## Running

From the `money-mate-api-token-exchange` directory:

```bash
../mvnw spring-boot:run
```

## Configuration

See `src/main/resources/application.yaml` for configuration options.

Available profiles:
- `local` - Local OBP instance (requires Docker)
- `public-sandbox` - OBP public sandbox

## Development Resources

- `../obp/http-client/` - shared HTTP request collection for testing OBP directly
- `../obp/sandbox/` - shared Docker Compose setup for a local OBP instance
- `money-mate-api.http` - IntelliJ HTTP Client file that uses native OAuth against `identity-broker`
- `http-client.env.json` - IntelliJ HTTP Client OAuth configuration for the token-exchange demo
