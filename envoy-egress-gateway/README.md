# Envoy Egress Gateway

Local Envoy egress gateway demo for Money Mate.

## Purpose

This module hosts the external authorization service that Envoy calls before
forwarding outbound OBP traffic. The auth service exchanges the incoming bearer
token with `identity-broker` and rewrites the outbound authorization header to
OBP DirectLogin format.

## Flow

`money-mate-api-forward-auth -> envoy-egress-gateway -> OBP`

- the API sends `Authorization: Bearer ...` to Envoy
- Envoy calls `http://127.0.0.1:10003/authorize`
- the auth service exchanges that bearer token at `identity-broker`
- the auth service returns `Authorization: DirectLogin token=<obp-token>`
- Envoy forwards the original request to OBP with the rewritten header

## Run

Start the auth service:

```bash
./run-authz.sh
```

Start Envoy in a second terminal:

```bash
./run-envoy.sh
```
