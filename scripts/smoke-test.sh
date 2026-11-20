#!/usr/bin/env bash
set -euo pipefail

FRONTEND_URL="${FRONTEND_URL:-https://comunidad360-web-q2ln.onrender.com}"
API_URL="${API_URL:-https://comunidad360-api-ihic.onrender.com}"
TIMEOUT="${SMOKE_TIMEOUT:-60}"
RETRIES="${SMOKE_RETRIES:-3}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass=0
fail=0
ok() { echo "PASS  $1"; pass=$((pass + 1)); }
ko() { echo "FAIL  $1"; fail=$((fail + 1)); }

get() {
  curl -sS -m "$TIMEOUT" -o "$TMP/body" -w '%{http_code}' "$1"
}

echo "Smoke test COMUNIDAD 360"
echo "  Frontend: $FRONTEND_URL"
echo "  API:      $API_URL"
echo

frontend_ok=0
for _ in $(seq 1 "$RETRIES"); do
  code="$(get "$FRONTEND_URL/" || echo 000)"
  if [[ "$code" == "200" ]] && grep -q 'id="root"' "$TMP/body"; then
    frontend_ok=1
    break
  fi
  sleep 5
done
if [[ "$frontend_ok" == "1" ]]; then
  ok "frontend responde 200 y sirve la app"
else
  ko "frontend no responde o no sirve la app"
fi

health_ok=0
for _ in $(seq 1 "$RETRIES"); do
  code="$(get "$API_URL/actuator/health" || echo 000)"
  if [[ "$code" == "200" ]] && grep -q '"status":"UP"' "$TMP/body"; then
    health_ok=1
    break
  fi
  sleep 5
done
if [[ "$health_ok" == "1" ]]; then
  ok "API health = UP"
else
  ko "API health no está UP (cold start o caída)"
fi

origin="${FRONTEND_URL%/}"
acao="$(curl -sS -m "$TIMEOUT" -i -X OPTIONS "$API_URL/api/v1/auth/login" \
  -H "Origin: $origin" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" 2>/dev/null \
  | tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin"{print $2}' | head -1)"
if [[ "$acao" == "$origin" ]]; then
  ok "CORS permite el origen del frontend"
else
  ko "CORS no permite $origin (recibido: '${acao:-ninguno}')"
fi

login_code="$(curl -sS -m "$TIMEOUT" -o /dev/null -w '%{http_code}' -X POST \
  "$API_URL/api/v1/auth/login" \
  -H "Origin: $origin" \
  -H 'Content-Type: application/json' \
  --data '{"email":"smoke@example.com","password":"WrongPassword123!"}' 2>/dev/null || echo 000)"
if [[ "$login_code" == "401" ]]; then
  ok "login responde 401 con credenciales inválidas (sin error de CORS/servidor)"
elif [[ "$login_code" == "429" ]]; then
  ok "login responde 429 (rate limit activo)"
else
  ko "login devolvió $login_code (esperado 401 o 429)"
fi

echo
echo "Resultado: $pass OK, $fail fallos"
[[ "$fail" -eq 0 ]]
