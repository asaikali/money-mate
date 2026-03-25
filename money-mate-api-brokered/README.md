# Money Mate API Brokered

Spring Boot HATEOAS backend providing the brokered-identity variant of the Money Mate API.

## Description

This module is scaffolded from `money-mate-api` and will evolve into the version that expects brokered bearer tokens instead of direct username/password login.

At this stage it is only a starting point:
- the existing HATEOAS surface is copied over
- the direct-login flow is still present
- token exchange and brokered resource-server security are not implemented yet

## Technology Stack

- Spring Boot 4.0.4
- Spring HATEOAS (HAL+Forms)
- Spring Web
- Java 25

## Running

From the `money-mate-api-brokered` directory:

```bash
../mvnw spring-boot:run
```

## Configuration

See `src/main/resources/application.yaml` for configuration options.

Available profiles:
- `local` - Local OBP instance (requires Docker)
- `public-sandbox` - OBP public sandbox

## Development Resources

- `obp-api/` - HTTP request collection for testing OBP API
- `sandbox/` - Docker compose for local OBP instance
