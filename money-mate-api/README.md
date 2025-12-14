# Money Mate API

Spring Boot HATEOAS backend providing a hypermedia-driven banking API.

## Description

This module adapts the Open Bank Project (OBP) API into an agent-friendly hypermedia API using HAL+Forms. It knows nothing about AI or agents - it simply provides a discoverable, self-describing REST API.

## Technology Stack

- Spring Boot 3.5.8
- Spring HATEOAS (HAL+Forms)
- Spring Web
- Java 25

## Running

From the money-mate-api directory:

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
