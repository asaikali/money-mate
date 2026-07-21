# Money Mate API — Design

## What this API is

Money Mate API is a HAL+Forms hypermedia API for personal banking aggregation.
It surfaces a user's accounts, balances, banks, and transactions over Open
Bank Project (OBP), and is shaped from the ground up so an AI agent can drive
it without prior knowledge of the route table.

There are three conventions:

- **Discover navigation through `_links`.** Each response advertises related
  resources that are reachable from the current representation, so a client
  need not construct endpoint URLs from prior knowledge.
- **Discover transitions through `_templates`.** State-changing operations
  (POST, PUT, DELETE, PATCH) are described as HAL-FORMS templates: a method,
  a target URL, and a property list. If a template is absent, that transition
  is not advertised by the current representation.
- **Use the API profile for additional semantics.** The root response carries
  a `profile` link to `/AGENTS.md`, which documents Money Mate's HAL-FORMS
  conventions without determining the caller's goal.

The rest of this document walks the API the way an agent walks it — one
request, one response at a time — and names each piece as it appears.

---

## The client's entry point: `GET /`

A client that knows the API's base URL and nothing else can start with one
request:

```http
GET / HTTP/1.1
Accept: application/prs.hal-forms+json
Host: localhost:8080
```

The response is a HAL+Forms document. It carries no business data — it is
pure handshake.

```json
{
  "_links": {
    "self":    {"href": "http://localhost:8080/"},
    "profile": {"href": "/AGENTS.md", "title": "Money Mate HAL-FORMS Profile", "type": "text/markdown"},
    "about":   {"href": "/docs/api",  "title": "Money Mate API Overview",        "type": "text/markdown"}
  },
  "_templates": {
    "createSession": {
      "method": "POST",
      "target": "http://localhost:8080/session",
      "title":  "Login (establish authenticated session)",
      "properties": [
        {"name": "username", "prompt": "Email address", "required": true, "regex": "^(?=\\s*\\S).*$"},
        {"name": "password", "prompt": "Password",      "required": true, "regex": "^(?=\\s*\\S).*$"}
      ]
    }
  },
  "api_usage": "This API uses HAL-FORMS. The current representation advertises related resources in _links and available state transitions in _templates. Additional conventions are documented by _links.profile."
}
```

The shape of that response provides the vocabulary a client needs to begin
discovery.

`_links` is an object whose keys are link relation types. The link relation
framework comes from [RFC 8288 — Web Linking][rfc8288]; a relation names *the
kind of relationship* between the current resource and another (`profile`
means "documentation about how to process this resource," `self` means "this
resource's canonical URL"). The [HAL specification][hal] defines the
`_links` JSON shape; Spring HATEOAS produces these objects via `Link.of(...)`
and `LinkRelation` helpers.

`_templates` is the [HAL-FORMS][hal-forms] extension. Where `_links`
describes "where you can go," `_templates` describes the transitions currently
offered by a resource. Each entry is one operation: an HTTP method, a target
URL, and the input properties. A template advertises a transition; its absence
means that transition is not advertised by the current representation. The
server still enforces authentication, authorization, and validation when a
request is submitted. The `createSession` template above is the state-changing
operation advertised to an unauthenticated caller, including its method,
target, and required fields.

The `api_usage` field gives a compact description of those representation
semantics. The `profile` and `about` links provide the additional API-specific
conventions and domain context.

---

## Reading the profile: `profile` → `/AGENTS.md`

[RFC 6906 — The 'profile' Link Relation Type][rfc6906] defines the `profile`
relation as pointing to additional semantics and processing rules beyond
what the media type alone conveys. That is exactly the use here. The
HAL-FORMS media type defines the general link and template structures; the
profile describes how this API uses those structures.

A client can retrieve the profile through the advertised link:

```http
GET /AGENTS.md HTTP/1.1
Accept: text/markdown
```

The response is plain markdown. `text/markdown` keeps the conventions readable
by people and language-model clients without requiring a JSON-schema layer.
It explains that links advertise navigation, templates advertise available
state transitions, and the server remains responsible for authentication,
authorization, and validation. It also makes clear that the caller continues
to determine the goal of each interaction.

The profile is served from `ApiRootController` as a concise embedded string
constant.

---

## Reading the context: `about` → `/docs/api`

[RFC 6903 §2 — 'about' Link Relation][rfc6903] defines `about` as pointing
to "a resource that is the subject of the link's context." Where `profile`
adds representation conventions, `about` says *what this thing is.*

```http
GET /docs/api HTTP/1.1
Accept: text/markdown
```

The response describes Money Mate at the domain level: the core resources
(User, Accounts, Banks, Transactions), the fact that authentication is
required, and that available state transitions are advertised through
`_templates`. It does not list endpoints; clients can discover current URLs
from `_links` in subsequent responses.

The split between `profile` and `about` follows the RFCs' separation of
concerns: `profile` describes representation conventions, while `about`
describes domain semantics. The API can revise either document independently.

---

## Acting: the `createSession` template

After reading the root representation and its context, a client can select the
advertised `createSession` template when login matches the caller's goal. The
client fills in the template's properties and submits an HTTP request matching
its `method` and `target`:

```http
POST /session HTTP/1.1
Content-Type: application/json
Accept: application/prs.hal-forms+json

{"username": "timo.fi.29@example.com", "password": "6addcd"}
```

Two things to note.

The login URL never appeared as a `_links` entry. It appeared *only* as the
`target` of a template. That is deliberate: links are for navigating to
existing resources; templates are for creating or modifying them. A login is
the act of creating a session, so it is a template, attached to the root
resource as an affordance on its self-link.

The template properties (`username`, `password`) are typed and validated
server-side. The `regex` on each property gives the agent enough of a hint
to validate input before submission, without the API having to expose a full
schema language.

The successful response:

```http
HTTP/1.1 201 Created
Location: /session
Content-Type: application/prs.hal-forms+json
Cache-Control: no-store, private

{
  "_links": {
    "me":    {"href": "http://localhost:8080/users/me",   "title": "Your user profile and available actions"},
    "self":  {"href": "http://localhost:8080/session"},
    "about": {"href": "/docs/session", "title": "Session authentication and lifecycle", "type": "text/markdown"},
    "root":  {"href": "/",             "title": "Return to API root"}
  },
  "access_token": "MMAT-94c533f9-6b65-404a-bca6-a1f8bf97f343",
  "token_type":   "Bearer"
}
```

The `access_token` is an opaque, server-minted string with the `MMAT-`
prefix (Money Mate Access Token). It is not a JWT — the token has no
meaning outside this API; the session documentation describes it as opaque.
Server-side, the token is held in `SessionTokenStore`, which
maps the opaque string to the upstream OBP credential.

The agent now switches into authenticated mode by attaching an
[RFC 6750 — OAuth 2.0 Bearer Token Usage][rfc6750] header on subsequent
requests:

```
Authorization: Bearer MMAT-94c533f9-6b65-404a-bca6-a1f8bf97f343
```

If a request to a protected resource omits the header, presents a malformed
one, or presents a revoked or expired token, the API responds with `401
Unauthorized` and a `WWW-Authenticate: Bearer` header — the standard
challenge defined by RFC 6750 and [RFC 9110 — HTTP Semantics][rfc9110]. The
root resource advertises the currently available authentication controls, so
a client can rediscover login after a 401 when that matches the caller's goal.
There is no refresh flow.

The session response also carries an `about` link to `/docs/session`, a
markdown document covering session semantics: how to attach the token, that
the token is opaque, when 401s occur, and how the `deleteSession` template
advertises logout.

---

## Following links into authenticated state

The session response advertised four relations: `me`, `self`, `about`,
`root`. Notice what is *not* there: there is no `accounts` link directly on
the session resource. The agent reaches accounts by following `me` to the
user resource first.

```http
GET /users/me HTTP/1.1
Authorization: Bearer MMAT-…
Accept: application/hal+json
```

```json
{
  "_links": {
    "self":     {"href": "http://localhost:8080/users/me"},
    "root":     {"href": "/"},
    "accounts": {"href": "/accounts", "title": "All my accounts"},
    "banks":    {"href": "/banks",    "title": "Banks I bank with"}
  },
  "username": "timo.fi.29@example.com",
  "email":    "timo.fi.29@example.com",
  "accountCount": 4,
  "bankCount":    1
}
```

This is where the structure starts to pay off. The user resource exposes the
`accounts` and `banks` relations. The agent does not need to know the URL
is `/accounts`; it follows the relation. If we moved the route to
`/v2/accounts` tomorrow, the agent would follow the new URL without any
client-side change.

The same property holds for `GET /session`, which the agent can reach via
the `self` link returned by login:

```json
{
  "_links": {
    "self":  {"href": "http://localhost:8080/session"},
    "about": {"href": "/docs/session", "type": "text/markdown", "title": "Session authentication and lifecycle"},
    "me":    {"href": "/users/me"},
    "root":  {"href": "/"}
  },
  "tokenType": "Bearer"
}
```

The session resource carries an *affordance* on its self-link — a HAL-FORMS
template named `deleteSession`. Spring HATEOAS attaches affordances via
`andAffordance(afford(methodOn(...).deleteSession(null)))`; the resulting
template is rendered into the response under `_templates`. Agents that read
templates discover that logout is permitted, with method `DELETE` and target
`/session`.

Following `accounts` from `/users/me`:

```json
{
  "_links": {
    "self": {"href": "http://localhost:8080/accounts"},
    "root": {"href": "/", "title": "API root"},
    "me":   {"href": "/users/me", "title": "My profile"}
  },
  "accountCount": 4,
  "accounts": [
    {
      "id":          "80c546b0-b3e3-4aa0-b3f4-56084ee6cca2",
      "accountType": "CURRENT PLUS",
      "iban":        "FI12 1234 5123 4511 2693 7214 677",
      "currency":    "EUR",
      "amount":      "2622.67",
      "_links": {
        "self":         {"href": "/accounts/80c546b0-…",                "title": "Account details"},
        "bank":         {"href": "/banks/gh.29.fi",                     "title": "fi"},
        "transactions": {"href": "/accounts/80c546b0-…/transactions",   "title": "Transactions"},
        "balance":      {"href": "/accounts/80c546b0-…/balance",        "title": "Balance"}
      }
    }
  ]
}
```

Each account carries its own `_links` block. This is the recursive structure
of HAL: an embedded resource is itself a hypermedia resource, with its own
self URL and its own outbound relations. The agent does not have a global
URL template like `/accounts/{id}/transactions`; it has *the actual URL for
each specific account's transactions*, returned to it in this response.

---

## Following a discovered URL

The point of HAL becomes crisp at the next step. The agent picks an account
from the collection and fetches its transactions:

```http
GET /accounts/80c546b0-b3e3-4aa0-b3f4-56084ee6cca2/transactions HTTP/1.1
Authorization: Bearer MMAT-…
Accept: application/hal+json
```

There is no way for the agent to have written that URL in advance. The
account ID is a UUID generated upstream, returned to the agent in the
previous response, and used immediately as the basis of the next request.
This is the difference between *integration through documentation* — where
a developer reads a Swagger spec and hand-codes a URL template — and
*integration through hypermedia* — where the server hands the client the
exact URL to use.

The transactions response is again a HAL collection:

```json
{
  "transactionCount": 50,
  "transactions": [
    {
      "date":         "2017-05-11T06:34:51Z",
      "description":  "Test transfer",
      "amount":       "10.00",
      "currency":     "EUR",
      "balanceAfter": "2622.67",
      "_links": {
        "self":    {"href": "/accounts/80c546b0-…/transactions/4a230217-…", "title": "Transaction details"},
        "account": {"href": "/accounts/80c546b0-…",                          "title": "Account"}
      }
    }
  ],
  "_links": {
    "self":    {"href": "/accounts/80c546b0-…/transactions"},
    "account": {"href": "/accounts/80c546b0-…", "title": "Back to account"},
    "root":    {"href": "/", "title": "API root"}
  }
}
```

Each transaction has a `self` link to its detail resource and an `account`
link back to the parent account. An agent that wants to investigate a single
transaction has the URL handed to it; an agent that wants to navigate back
up has the URL handed to it. The collection carries its own `self` for
re-fetching plus `account` and `root` for context.

---

## Ending: the `deleteSession` template

The agent has done what it came to do. To finish, it logs out using the
`deleteSession` template advertised on `GET /session`:

```http
DELETE /session HTTP/1.1
Authorization: Bearer MMAT-…
```

```http
HTTP/1.1 204 No Content
```

The token is revoked server-side, and any subsequent request bearing it
returns `401 Unauthorized`, exactly as if the token had never existed.

Note that logout is exposed as a *template*, not a *link*. Templates
describe state changes; logout terminates the session resource, so it is a
`DELETE` operation on `/session`, surfaced as a HAL-FORMS template attached
to the session resource's self-link. There is no global `/logout` URL.

---

## Notes for API authors

A few invariants this API follows that are not obvious from any single
response.

**Affordances are state-specific.** The root resource at `/` returns
different templates depending on whether the caller is authenticated.
Unauthenticated callers see `createSession`; authenticated callers see
`deleteSession`. This is enforced in `ApiRootController` by inspecting
`SecurityContextHolder` before building the response. The agent does not
need to "know its state" — the response tells it.

**The token store is the only stateful component.** Spring Security is
configured `STATELESS` (no server-side HTTP session). `SessionTokenStore`
maps the opaque `MMAT-…` token to a `SessionPrincipal`, which carries the
upstream OBP token. Every authenticated request flows through
`UuidBearerTokenAuthFilter`, which reads `Authorization: Bearer …`, looks
up the token, and populates the security context. Logout calls
`tokenStore.revoke(token)` and that is the entire teardown.

**`profile` and `about` are link relations, not URL templates.** They
happen to point at `/AGENTS.md` and `/docs/api` today. They could point at
versioned URLs (`/AGENTS.md/v2`), or at completely different paths, without
any change to a client that resolves advertised relations instead of relying
on hard-coded paths.

**Every response is a complete decision point.** A correctly written agent
can drop the previous response from its context after each request, keeping
only the new one. Each response carries enough `_links` and `_templates` to
decide what to do next. There is no "remember the URL pattern from earlier"
requirement, which is what keeps the context window bounded as the agent
navigates deep into the API.

**Collections embed children with full hypermedia.** The accounts collection
does not return bare IDs that the client must combine with a URL template —
it returns full URLs in each child's `_links` block. This is what lets an
agent navigate deeply (account → transactions → individual transaction →
back to account) without ever constructing a URL.

---

## References

### RFCs

- [RFC 8288 — Web Linking][rfc8288]. The link relation framework that
  `_links` is built on.
- [RFC 6906 — The 'profile' Link Relation Type][rfc6906]. Used here to
  point at the Money Mate HAL-FORMS profile.
- [RFC 6903 §2 — 'about' Link Relation][rfc6903]. Used here to point at
  API and session context documents.
- [RFC 6750 — OAuth 2.0 Bearer Token Usage][rfc6750]. The `Authorization:
  Bearer …` and `WWW-Authenticate: Bearer` semantics this API follows.
- [RFC 9110 — HTTP Semantics][rfc9110]. The current authoritative source
  for HTTP method and status code semantics.

### Wire formats and registries

- [HAL specification][hal] — `application/hal+json`.
- [HAL-FORMS specification][hal-forms] — `application/prs.hal-forms+json`.
- [IANA Link Relations Registry][iana-link-rels].

### Implementation

- [Spring HATEOAS Reference][spring-hateoas].

[rfc8288]:        https://www.rfc-editor.org/rfc/rfc8288
[rfc6906]:        https://www.rfc-editor.org/rfc/rfc6906
[rfc6903]:        https://www.rfc-editor.org/rfc/rfc6903#section-2
[rfc6750]:        https://www.rfc-editor.org/rfc/rfc6750
[rfc9110]:        https://www.rfc-editor.org/rfc/rfc9110
[hal]:            https://stateless.group/hal_specification.html
[hal-forms]:      https://rwcbook.github.io/hal-forms/
[iana-link-rels]: https://www.iana.org/assignments/link-relations/link-relations.xhtml
[spring-hateoas]: https://docs.spring.io/spring-hateoas/docs/current/reference/html/
