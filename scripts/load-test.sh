#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
EMAIL="${LOAD_TEST_EMAIL:-admin@comunidad360.local}"
PASSWORD="${LOAD_TEST_PASSWORD:-Admin1234!}"
HEALTH_REQUESTS="${HEALTH_REQUESTS:-300}"
HEALTH_CONCURRENCY="${HEALTH_CONCURRENCY:-10}"
DASHBOARD_REQUESTS="${DASHBOARD_REQUESTS:-200}"
DASHBOARD_CONCURRENCY="${DASHBOARD_CONCURRENCY:-5}"

TOKEN="$(curl -fsS -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  "$BASE_URL/api/v1/auth/login" | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')"
CID="$(curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/me" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["memberships"][0]["id"])')"

echo "Health: $HEALTH_REQUESTS requests, concurrency $HEALTH_CONCURRENCY"
ab -q -n "$HEALTH_REQUESTS" -c "$HEALTH_CONCURRENCY" "$BASE_URL/actuator/health" \
  | grep -E 'Requests per second|Time per request|Failed requests|Non-2xx'

echo "Dashboard: $DASHBOARD_REQUESTS requests, concurrency $DASHBOARD_CONCURRENCY"
ab -q -n "$DASHBOARD_REQUESTS" -c "$DASHBOARD_CONCURRENCY" \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/v1/communities/$CID/dashboard" \
  | grep -E 'Requests per second|Time per request|Failed requests|Non-2xx'
