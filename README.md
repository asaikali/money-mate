# money-mate

*A project for experimenting with agent architectures using Spring AI, Spring HATEOAS, and external
REST APIs.*

Money Mate is a small Spring Boot application used to explore how AI-driven agents can interact with
REST APIs,
plan multi-step workflows, and reason about API responses at runtime. The goal is to provide a safe
environment for testing:

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

Use the included `docker-compose.yaml` from the `sandbox/` directory:

```shell
docker compose -f sandbox/docker-compose.yaml up -d
```

Or run from the sandbox directory:

```shell
cd sandbox
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
- **Consumer Key:** see "Obtaining a Consumer Key" below

These are sufficient for authentication experiments and agent workflows.

## **Obtaining a Consumer Key**

Before you can authenticate with OBP's DirectLogin API, you need to create a **consumer** (
application registration). This is a one-time manual step.

### Steps to Create a Consumer

1. **Open the OBP Consumer Registration page** in your browser:
[http://localhost:8081/consumer-registration](http://localhost:8081/consumer-registration)

2. **Login with the super admin credentials:**
    - Username: `admin@example.com`
    - Password: `admin123`

3. **Fill out the consumer registration form:**
    - **Application Name:** `money-mate`
    - **Application Type:** Select `Confidential`
    - **Description:** `Money Mate AI Aigent`
    - **Developer Email:** `admin@example.com`

4. **Click "Register Consumer"**

5. **Save the credentials** displayed on the confirmation page:
    - **Consumer Key** (40 characters, e.g., `g0vjy3au0j443wmc24amgdlrngtdszwwe4gmkrxr`)
    - **Consumer Secret** (40 characters, e.g., `04kjmk2413w1bhtakwhhlufj3iunkfui0zz5224e`)
    - **Consumer ID** (UUID)

6. **Update your `.env` file** with the consumer credentials:
   ```bash
   OBP_CONSUMER_KEY=<paste-your-consumer-key>
   OBP_CONSUMER_SECRET=<paste-your-consumer-secret>
   ```

7. **Test your consumer** using the requests in `obp-api/`:
    - Open `obp-api/00-auth.http` in your IDE
    - Select the `local` environment from the dropdown
    - Run the "DirectLogin - Get Token" request
    - You should receive a JWT token in the response

### Example Consumer Registration Response

After successful registration, you'll see something like this:

```
Register your consumer
Thanks for registering your consumer with the Open Bank Project API!

Consumer ID: dba56122-be42-402a-aaa3-bf552173a0a1
Application Type: Confidential
Application Name: money-mate-app
Consumer Key: g0vjy3au0j443wmc24amgdlrngtdszwwe4gmkrxr
Consumer Secret: 04kjmk2413w1bhtakwhhlufj3iunkfui0zz5224e
Direct Login Endpoint: http://localhost:8081/my/logins/direct
```

**Important:** Save both the Consumer Key and Consumer Secret securely. The Consumer Secret may not
be shown again.

Once you have these credentials, you can use DirectLogin for all API calls and even create
additional consumers programmatically via the `/management/consumers` endpoint.

