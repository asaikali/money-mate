#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080"
USERNAME="timo.fi.29@example.com"
PASSWORD="6addcd"

# Persist the session token to disk so CLI-mode calls (one function per
# process) can share state across invocations, e.g. `./test.sh login` then
# `./test.sh current_user` in a later shell.
TOKEN_FILE="${TOKEN_FILE:-${TMPDIR:-/tmp}/money-mate.session}"
SESSION_TOKEN="${SESSION_TOKEN:-$( [[ -f "$TOKEN_FILE" ]] && cat "$TOKEN_FILE" || true )}"

httpv() {
  http --ignore-stdin --verbose "$@"
}

require_token() {
  if [[ -z "${SESSION_TOKEN}" ]]; then
    echo "No SESSION_TOKEN set. Run: login"
    return 1
  fi
}

# HTTPie --verbose emits CRLF (HTTP wire format), so strip \r before using
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

# Re-fetch /accounts quietly, pluck the first account's transactions href out
# of the response, then GET it. This demonstrates discovery through
# _links.transactions on accounts[0] without relying on a hard-coded URL.
transactions() {
  require_token

  local accounts_body href
  accounts_body="$(
    http --ignore-stdin GET "${BASE_URL}/accounts" \
      Authorization:"Bearer ${SESSION_TOKEN}" \
      Accept:application/hal+json
  )"

  href="$(echo "$accounts_body" | jq -r '.accounts[0]._links.transactions.href')"
  if [[ -z "$href" || "$href" == "null" ]]; then
    echo "No transactions link found on accounts[0]."
    return 1
  fi

  echo "Discovered transactions link on accounts[0]: ${href}"
  echo

  # Hrefs may be absolute or root-relative; normalize before requesting.
  local url
  if [[ "$href" == http* ]]; then
    url="$href"
  else
    url="${BASE_URL}${href}"
  fi

  httpv GET "$url" \
    Authorization:"Bearer ${SESSION_TOKEN}" \
    Accept:application/hal+json
}

logout() {
  require_token
  httpv DELETE "${BASE_URL}/session" Authorization:"Bearer ${SESSION_TOKEN}"
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
  transactions; pause
  logout
}

menu() {
  cat <<EOF

Money Mate HATEOAS API demo  -  hypermedia walkthrough

BASE_URL=${BASE_URL}

  Step  Request                            Why this step
  ----  ---------------------------------  -------------------------------------
   1    GET    /                           bootstrap: discover the API
   2    GET    /AGENTS.md                  follow  profile       link (HAL-FORMS profile)
   3    GET    /docs/api                   follow  about         link (semantics)
   4    POST   /session                    use     createSession template (login)
   5    GET    /docs/session               follow  about         link on session
   6    GET    /session                    follow  self          link (session status)
   7    GET    /users/me                   follow  me            link
   8    GET    /accounts                   follow  accounts      link on /users/me
   9    GET    /accounts/.../transactions  follow  transactions  link on first account
  10    DELETE /session                    use     deleteSession template (logout)

  all   run the full walkthrough      q   quit

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
      9) transactions ;;
      10) logout ;;
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
