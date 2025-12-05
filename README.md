# money-mate

*A project for experimenting with agent architectures using Spring AI, Spring HATEOAS, and external REST APIs.*

Money Mate is a small Spring Boot application used to explore how AI-driven agents can interact with REST APIs, 
plan multi-step workflows, and reason about API responses at runtime. The goal is to provide a safe environment for testing:

- Agent architecture ideas
- HATEOAS resource modeling that guides agentic loops 
- External service interaction using real banking-domain APIs

This is a **learning and experimentation project**, not a production financial application.

## **Open Bank Project (OBP)**

Money Mate uses the **Open Bank Project** (OBP) as a realistic external REST API.

Key properties:

- **REST API** (JSON over HTTP)
- Provides realistic banking-domain endpoints:
    - Accounts
    - Transactions
    - Customers
    - Views & permissions
    - Payments
- Provides relastic dummy data that enables experimentation with agent workflows

## **Running OBP with Docker Compose**

Use the included `docker-compose.yaml`:

```shell
docker compose up -d
```

## **Validate the OBP API Is Running**

After starting the stack, validate it by calling `/root`:

```shell
curl http://localhost:8081/obp/v6.0.0/root
```

You should receive basic OBP information such as:
- API version
- Git commit

If this endpoint responds, OBP is successfully running.

## **Default Credentials**

For convenience, the local OBP instance includes default demo credentials:

- **Username:** `admin@example.com`
- **Password:** `admin123`
- **Consumer Key:** set in your compose environment (`consumer_key`)

These are sufficient for authentication experiments and agent workflows.

