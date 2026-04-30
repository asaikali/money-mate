#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080"
USERNAME="timo.fi.29@example.com"
PASSWORD="6addcd"

# Persist tokens to disk so CLI-mode calls (one function per process)
# can share state across invocations, e.g. `./test2.sh login` then
# `./test2.sh current_user` in a later shell.
TOKEN_FILE="${TOKEN_FILE:-${TMPDIR:-/tmp}/money-mate.session}"
TOKEN_FILE_2="${TOKEN_FILE_2:-${TMPDIR:-/tmp}/money-mate.session2}"

SESSION_TOKEN="${SESSION_TOKEN:-$( [[ -f "$TOKEN_FILE" ]] && cat "$TOKEN_FILE" || true )}"
SESSION_TOKEN_2="${SESSION_TOKEN_2:-$( [[ -f "$TOKEN_FILE_2" ]] && cat "$TOKEN_FILE_2" || true )}"

httpv() {
  http --ignore-stdin --verbose "$@"
}

require_token() {
  if [[ -z "${SESSION_TOKEN}" ]]; then
    echo "No SESSION_TOKEN set. Run: login"
    return 1
  fi
}

# Extract response body from HTTPie --verbose output.
# HTTPie emits CRLF line endings (wire format), so strip \r before using
# awk paragraph mode. The response body is always the last paragraph.
extract_body() {
  tr -d '\r' | awk 'BEGIN{RS=""} END{print}'
}

pause() {
  echo
  read -r -p "Press Enter to continue..."
  echo
}

root_unauthenticated() {
  httpv GET "${BASE_URL}/"
}

agents_md() {
  httpv GET "${BASE_URL}/AGENTS.md" Accept:text/markdown
}

api_docs() {
  httpv GET "${BASE_URL}/docs/api" Accept:text/markdown
}

login() {
  local response body

  response="$(
    httpv POST "${BASE_URL}/session" \
      Content-Type:application/json \
      Accept:application/prs.hal-forms+json \
      username="${USERNAME}" \
      password="${PASSWORD}"
  )"

  echo "$response"

  body="$(echo "$response" | extract_body)"

  SESSION_TOKEN="$(echo "$body" | jq -r '.access_token')"

  export SESSION_TOKEN
  printf '%s' "$SESSION_TOKEN" > "$TOKEN_FILE"
  echo
  echo "SESSION_TOKEN=${SESSION_TOKEN}"
  echo "(persisted to ${TOKEN_FILE})"
}

session_docs() {
  httpv GET "${BASE_URL}/docs/session" Accept:text/markdown
}

session_status() {
  require_token
  httpv GET "${BASE_URL}/session" Authorization:"Bearer ${SESSION_TOKEN}"
}

current_user() {
  require_token
  httpv GET "${BASE_URL}/users/me" \
    Authorization:"Bearer ${SESSION_TOKEN}" \
    Accept:application/hal+json
}

accounts() {
  require_token
  httpv GET "${BASE_URL}/accounts" Authorization:"Bearer ${SESSION_TOKEN}"
}

root_authenticated() {
  require_token
  httpv GET "${BASE_URL}/" \
    Authorization:"Bearer ${SESSION_TOKEN}" \
    Accept:application/prs.hal-forms+json
}

logout() {
  require_token
  httpv DELETE "${BASE_URL}/session" Authorization:"Bearer ${SESSION_TOKEN}"
}

current_user_after_logout() {
  require_token
  httpv GET "${BASE_URL}/users/me" \
    Authorization:"Bearer ${SESSION_TOKEN}" \
    Accept:application/hal+json
}

invalid_token() {
  httpv GET "${BASE_URL}/users/me" \
    Authorization:"Bearer INVALID-TOKEN-12345" \
    Accept:application/hal+json
}

missing_authorization() {
  httpv GET "${BASE_URL}/session" Accept:application/hal+json
}

login2() {
  local response body

  response="$(
    httpv POST "${BASE_URL}/session" \
      Content-Type:application/json \
      Accept:application/prs.hal-forms+json \
      username="alice@example.com" \
      password="secret"
  )"

  echo "$response"

  body="$(echo "$response" | extract_body)"

  SESSION_TOKEN_2="$(echo "$body" | jq -r '.access_token')"

  export SESSION_TOKEN_2
  printf '%s' "$SESSION_TOKEN_2" > "$TOKEN_FILE_2"
  echo
  echo "SESSION_TOKEN_2=${SESSION_TOKEN_2}"
  echo "(persisted to ${TOKEN_FILE_2})"
}

current_user_token2() {
  if [[ -z "${SESSION_TOKEN_2}" ]]; then
    echo "No SESSION_TOKEN_2 set. Run: login2"
    return 1
  fi

  httpv GET "${BASE_URL}/users/me" \
    Authorization:"Bearer ${SESSION_TOKEN_2}" \
    Accept:application/hal+json
}

run_all() {
  root_unauthenticated; pause
  agents_md; pause
  api_docs; pause
  login; pause
  session_docs; pause
  session_status; pause
  current_user; pause
  accounts; pause
  root_authenticated; pause
  logout; pause
  current_user_after_logout; pause
  invalid_token; pause
  missing_authorization
}

menu() {
  cat <<EOF

Money Mate HATEOAS API demo

BASE_URL=${BASE_URL}

Commands:
  1   root_unauthenticated
  2   agents_md
  3   api_docs
  4   login
  5   session_docs
  6   session_status
  7   current_user
  8   accounts
  9   root_authenticated
  10  logout
  11  current_user_after_logout
  12  invalid_token
  13  missing_authorization
  14  login2
  15  current_user_token2
  all run_all
  q   quit

EOF
}

interactive() {
  while true; do
    menu
    read -r -p "money-mate> " choice

    case "$choice" in
      1) root_unauthenticated ;;
      2) agents_md ;;
      3) api_docs ;;
      4) login ;;
      5) session_docs ;;
      6) session_status ;;
      7) current_user ;;
      8) accounts ;;
      9) root_authenticated ;;
      10) logout ;;
      11) current_user_after_logout ;;
      12) invalid_token ;;
      13) missing_authorization ;;
      14) login2 ;;
      15) current_user_token2 ;;
      all) run_all ;;
      q|quit|exit) exit 0 ;;
      *) echo "Unknown command: $choice" ;;
    esac

    echo
  done
}

if [[ $# -eq 0 ]]; then
  interactive
else
  "$@"
fi
