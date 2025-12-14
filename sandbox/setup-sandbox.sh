#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-../.secrets/.env}"

echo "📄 Using env file: ${ENV_FILE}"
echo ""

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "❌ Env file not found: ${ENV_FILE}"
  echo "   Pass a different file as: ./setup-sandbox.sh my.env"
  exit 1
fi

# Load variables from .env
set -a
# shellcheck disable=SC1090
. "${ENV_FILE}"
set +a

# ---- Helpers -------------------------------------------------

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "❌ Required env var ${name} is not set"
    exit 1
  fi
}

# ---- Configuration -------------------------------------------

# Note: DirectLogin endpoint is NOT versioned, other endpoints are
OBP_HOST="${OBP_HOST:-http://localhost:8081}"
API_VERSION="${OBP_API_VERSION:-v5.0.0}"
API_BASE="${OBP_HOST}/obp/${API_VERSION}"

USERNAME="${OBP_USERNAME:-admin@example.com}"
PASSWORD="${OBP_PASSWORD:-admin123}"
SANDBOX_SECRET="${OBP_SANDBOX_DATA_IMPORT_SECRET:-mysecret}"

# ---- Check prerequisites -------------------------------------

require_var OBP_CONSUMER_KEY
require_var OBP_IMPORT_FILE

if [[ ! -f "${OBP_IMPORT_FILE}" ]]; then
  echo "❌ Import file not found: ${OBP_IMPORT_FILE}"
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "❌ jq is required but not installed. Please install jq."
  exit 1
fi

# ---- Step 1: Authenticate ------------------------------------

echo "🔐 Step 1/3: Authenticating with OBP API..."
echo "    URL: ${OBP_HOST}/my/logins/direct"

LOGIN_RESPONSE="$(
  curl -sS -X POST \
    "${OBP_HOST}/my/logins/direct" \
    -H "Content-Type: application/json" \
    -H "Authorization: DirectLogin username=${USERNAME},password=${PASSWORD},consumer_key=${OBP_CONSUMER_KEY}"
)"

if ! TOKEN="$(echo "${LOGIN_RESPONSE}" | jq -er '.token')" 2>/dev/null; then
  echo "❌ Failed to obtain DirectLogin token. Response was:"
  echo "${LOGIN_RESPONSE}" | jq . || echo "${LOGIN_RESPONSE}"
  exit 1
fi

echo "✅ Authenticated successfully"
echo ""

# ---- Step 2: Grant CanCreateSandbox role ---------------------

echo "🔑 Step 2/3: Granting CanCreateSandbox role..."

USER_RESPONSE="$(
  curl -sS -X GET \
    "${API_BASE}/users/current" \
    -H "Authorization: DirectLogin token=${TOKEN}"
)"

if ! USER_ID="$(echo "${USER_RESPONSE}" | jq -er '.user_id')" 2>/dev/null; then
  echo "❌ Failed to get user ID. Response was:"
  echo "${USER_RESPONSE}" | jq . || echo "${USER_RESPONSE}"
  exit 1
fi

echo "    User ID: ${USER_ID}"

GRANT_RESPONSE="$(
  curl -sS -X POST \
    "${API_BASE}/users/${USER_ID}/entitlements" \
    -H "Authorization: DirectLogin token=${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "bank_id": "",
      "role_name": "CanCreateSandbox"
    }'
)"

# Check if role was granted or already exists
if echo "${GRANT_RESPONSE}" | jq -e '.entitlement_id' >/dev/null 2>&1; then
  echo "✅ Role granted successfully"
elif echo "${GRANT_RESPONSE}" | jq -e '.message | contains("already")' >/dev/null 2>&1; then
  echo "✅ Role already exists (OK)"
else
  echo "⚠️  Role grant response:"
  echo "${GRANT_RESPONSE}" | jq . || echo "${GRANT_RESPONSE}"
  echo "    Continuing anyway..."
fi

echo ""

# ---- Step 3: Import sandbox data -----------------------------

echo "📥 Step 3/3: Importing sandbox data..."
echo "    File: ${OBP_IMPORT_FILE}"
echo "    URL:  ${API_BASE}/sandbox/data-import"

IMPORT_RESPONSE="$(
  curl -sS -X POST \
    "${API_BASE}/sandbox/data-import?secret=${SANDBOX_SECRET}" \
    -H "Content-Type: application/json" \
    -H "Authorization: DirectLogin token=${TOKEN}" \
    --data-binary @"${OBP_IMPORT_FILE}"
)"

# Check for errors
if echo "${IMPORT_RESPONSE}" | jq -e '.code' >/dev/null 2>&1; then
  ERROR_CODE="$(echo "${IMPORT_RESPONSE}" | jq -r '.code')"
  ERROR_MSG="$(echo "${IMPORT_RESPONSE}" | jq -r '.message')"

  echo ""
  echo "❌ Import failed with error ${ERROR_CODE}:"
  echo "   ${ERROR_MSG}"
  echo ""
  echo "Full response:"
  echo "${IMPORT_RESPONSE}" | jq .
  exit 1
fi

echo "✅ Import successful!"
echo ""
echo "Response:"
echo "${IMPORT_RESPONSE}" | jq . || echo "${IMPORT_RESPONSE}"
echo ""
echo "🎉 Sandbox setup complete!"
