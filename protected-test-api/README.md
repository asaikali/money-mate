# protected-test-api

Manual test guide for Phase 1 OAuth Device Flow using `http` (HTTPie) with visible output, plus `jq` for extracting values.

## Prerequisites

1. Java 25 and Maven wrapper (`./mvnw`)
2. `http` command (HTTPie) installed
3. `jq` installed
4. Two terminals

## Scripted Manual Walkthrough

You can run the helper script to walk the flow step by step (it does not start any app process):

```shell
./test-device-flow.sh
```

Optional overrides:

```shell
BROKER_BASE_URL=http://localhost:9000 \
PROTECTED_API_BASE_URL=http://localhost:8082 \
CLIENT_ID=protected-test-api-hypermedia-client \
SCOPE=hypermedia.access \
./test-device-flow.sh
```

## Default local setup

1. `identity-broker`: `http://localhost:9000`
2. `protected-test-api`: `http://localhost:8082`
3. Broker login user:
   - username: `demo`
   - password: `demo`

## 1. Start both apps

From repo root, in terminal 1:

```shell
./mvnw -pl identity-broker spring-boot:run
```

In terminal 2:

```shell
./mvnw -pl protected-test-api spring-boot:run
```

## 2. Inspect protected API root (human-readable first)

Run this first to see the HAL-FORMS bootstrap:

```shell
http GET :8082/ Accept:application/prs.hal-forms+json
```

You should see these expected values in the response:

1. discovery metadata: `http://localhost:9000/.well-known/openid-configuration`
2. device authorization endpoint: `http://localhost:9000/oauth2/device_authorization`
3. `client_id`: `protected-test-api-hypermedia-client`
4. `scope`: `hypermedia.access`
5. protected resource metadata link: `/.well-known/oauth-protected-resource`

## 3. Inspect auth server metadata (human-readable first)

Run this first to understand what the broker publishes:

```shell
http GET :9000/.well-known/openid-configuration
```

For local dev, these well-known endpoints are fixed:

1. Device authorization: `http://localhost:9000/oauth2/device_authorization`
2. Token endpoint: `http://localhost:9000/oauth2/token`

Protected resource metadata endpoint:

```shell
http GET :8082/.well-known/oauth-protected-resource
```

## 4. Start device authorization

Run request and inspect response:

```shell
http --form POST http://localhost:9000/oauth2/device_authorization \
  client_id=protected-test-api-hypermedia-client \
  scope=hypermedia.access
```

Extract values:

```shell
DEVICE_JSON=$(http --pretty=none --body --form POST http://localhost:9000/oauth2/device_authorization \
  client_id=protected-test-api-hypermedia-client \
  scope=hypermedia.access)
DEVICE_CODE=$(echo "$DEVICE_JSON" | jq -r '.device_code')
USER_CODE=$(echo "$DEVICE_JSON" | jq -r '.user_code')
VERIFICATION_URI=$(echo "$DEVICE_JSON" | jq -r '.verification_uri')
VERIFICATION_URI_COMPLETE=$(echo "$DEVICE_JSON" | jq -r '.verification_uri_complete // empty')
INTERVAL=$(echo "$DEVICE_JSON" | jq -r '.interval // 5')
echo "DEVICE_CODE=$DEVICE_CODE"
echo "USER_CODE=$USER_CODE"
echo "VERIFICATION_URI=$VERIFICATION_URI"
echo "VERIFICATION_URI_COMPLETE=$VERIFICATION_URI_COMPLETE"
echo "INTERVAL=$INTERVAL"
```

## 5. Complete login and consent in browser

1. Open `VERIFICATION_URI_COMPLETE` if it is present.
2. Otherwise open `VERIFICATION_URI` and enter `USER_CODE`.
3. Log in with `demo` / `demo`.
4. Approve consent.

## 6. Poll token endpoint and watch progress

Run this command, then rerun it every ~5 seconds until `ACCESS_TOKEN=` is non-empty:

```shell
TOKEN_JSON=$(http --pretty=none --body --form POST http://localhost:9000/oauth2/token \
  grant_type='urn:ietf:params:oauth:grant-type:device_code' \
  device_code="$DEVICE_CODE" \
  client_id=protected-test-api-hypermedia-client)
echo "$TOKEN_JSON" | jq .
ACCESS_TOKEN=$(echo "$TOKEN_JSON" | jq -r '.access_token // empty')
echo "ACCESS_TOKEN=$ACCESS_TOKEN"
```

Important: a device code is single-use after successful redemption. Do not call the token endpoint again with the same `device_code` after you already received an access token.

## 7. Call protected endpoint with token

First inspect response in formatted form:

```shell
http GET :8082/protected "Authorization:Bearer $ACCESS_TOKEN"
```

Expected:

1. HTTP 200
2. JSON contains `message`
3. JSON contains `timestamp`
4. JSON contains `user` (`subject`, `preferredUsername`, `audience`, `scope`)

## 8. Negative test (no token)

```shell
http GET :8082/protected
```

Expected:

1. HTTP 401
2. `WWW-Authenticate: Bearer resource_metadata="http://localhost/.well-known/oauth-protected-resource"`

## Common errors

1. `authorization_pending`: browser step is not complete yet.
2. `slow_down`: increase polling interval.
3. `expired_token`: restart from step 4.
4. `invalid_client`: confirm `client_id=protected-test-api-hypermedia-client`.
