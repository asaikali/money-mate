# Envoy Egress Gateway

Local Envoy egress gateway demo for Money Mate, backed by a Go gRPC
`ext_authz` service.

## Purpose

This directory hosts:

- a Go gRPC external authorization service under `authz-server`
- an Envoy config that fronts outbound OBP traffic on `127.0.0.1:10000`

Envoy calls the auth service before forwarding outbound OBP traffic. The auth
service exchanges the incoming bearer token with `identity-broker` and rewrites
the outbound authorization header to OBP DirectLogin format.

## Flow

`money-mate-api-forward-auth -> envoy-egress-gateway -> OBP`

- the API sends `Authorization: Bearer ...` to Envoy
- Envoy listens on `127.0.0.1:10000`
- Envoy calls the gRPC authz server on `127.0.0.1:10003`
- the auth service exchanges that bearer token at `identity-broker`
- the auth service returns `Authorization: DirectLogin token=<obp-token>`
- Envoy forwards the original request to OBP with the rewritten header

If token exchange fails, Envoy fails closed and returns `403`.

## Run

Start the Go auth service:

```bash
./run-authz.sh
```

Start Envoy in a second terminal:

```bash
./run-envoy.sh
```

Recommended order for local runs:

1. `identity-broker`
2. `./run-authz.sh`
3. `./run-envoy.sh`
4. `money-mate-api-forward-auth`

## Config

The Go auth service reads its settings from
[config.yaml](/Users/adib/dev/asaikali/money-mate/envoy-egress-gateway/authz-server/config.yaml).

Envoy reads its configuration from
[envoy.yaml](/Users/adib/dev/asaikali/money-mate/envoy-egress-gateway/envoy.yaml).

Key settings:

- `server.listen_address`: gRPC listen address for Envoy
- `server.shared_secret`: shared secret that Envoy passes as gRPC metadata
- `server.timeout`: HTTP timeout used by the Go auth service when calling `identity-broker`
- `identity_broker.token_uri`: OAuth token endpoint
- `identity_broker.token_exchange_client_id`: token exchange client id
- `identity_broker.token_exchange_client_secret`: token exchange client secret
- `envoy.yaml` `grpc_service.timeout`: Envoy timeout for the `ext_authz` call, currently `3s`

## Verify

Once both processes are running, this request should hit Envoy and the Go auth
service:

```bash
curl -v http://localhost:10000/obp/v5.1.0/my/accounts \
  -H 'Authorization: Bearer test'
```

Expected result:

- Envoy returns `403`
- the response body is `token exchange failed`
- `run-authz.sh` prints `CHECK:` and `TOKEN_EXCHANGE:` log lines
