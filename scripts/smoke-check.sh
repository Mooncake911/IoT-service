#!/bin/sh
# Smoke-check of the running IoT stack (bash port of smoke-check.ps1).
# Used by cd.yml after VM and K8s deploys; runnable locally too.
#
#   ./scripts/smoke-check.sh [gateway-url] [dashboard-url]
#
# Exit code != 0 on the first failed assertion.
set -eu

GATEWAY_URL="${1:-http://localhost:8085}"
DASHBOARD_URL="${2:-http://localhost:8501}"

assert_code() {
  name="$1"; code="$2"; expected="$3"
  if [ "$code" != "$expected" ]; then
    echo "FAIL: $name — HTTP $code (expected $expected)" >&2
    exit 1
  fi
  echo "OK: $name (HTTP $code)"
}

get_code() {
  curl -s -o /dev/null -w "%{http_code}" -X "$1" "$2"
}

echo "Checking gateway endpoints..."
assert_code "GET /api/v1/simulator/status" "$(get_code GET "$GATEWAY_URL/api/v1/simulator/status")" 200
assert_code "GET /api/v1/analytics/status" "$(get_code GET "$GATEWAY_URL/api/v1/analytics/status")" 200
assert_code "GET /api/v1/analytics/live/summary" "$(get_code GET "$GATEWAY_URL/api/v1/analytics/live/summary")" 200

echo "Configuring analytics method..."
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/analytics/config?method=Parallel&windowSeconds=50")
case "$code" in
  200|201|202) echo "OK: POST /api/v1/analytics/config (HTTP $code)" ;;
  *) echo "FAIL: POST /api/v1/analytics/config — HTTP $code" >&2; exit 1 ;;
esac

echo "Checking dashboard..."
assert_code "GET / (dashboard-ui)" "$(get_code GET "$DASHBOARD_URL")" 200

echo "POST $GATEWAY_URL/api/v1/controller"
payload_file="$(mktemp)"
trap 'rm -f "$payload_file"' EXIT INT TERM
cat > "$payload_file" <<'EOF'
[
  {
    "id": 101,
    "name": "sensor-101",
    "manufacturer": "acme",
    "type": "SENSOR_TEMPERATURE",
    "capabilities": ["temp"],
    "location": { "x": 1, "y": 2, "z": 3 },
    "status": {
      "isOnline": false,
      "batteryLevel": 3,
      "signalStrength": 5,
      "lastHeartbeat": "2026-04-26T08:00:00Z"
    }
  }
]
EOF
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" --data-binary "@$payload_file" "$GATEWAY_URL/api/v1/controller")
echo "Ingest status: $code"
case "$code" in
  200|201|202) ;;
  *) echo "FAIL: POST /api/v1/controller — HTTP $code" >&2; exit 1 ;;
esac

echo "Polling analytics history..."
# History is written once per analytics window (bufferTimeout): the config
# POST above restarts the window timer, so polling must cover windowSeconds
# (50s) plus margin — otherwise history is guaranteed to still be empty.
history=""
attempt=1
while [ "$attempt" -le 32 ]; do
  sleep 2
  history=$(curl -s "$GATEWAY_URL/api/v1/analytics/history?limit=5")
  if [ -n "$history" ] && [ "$history" != "[]" ]; then
    break
  fi
  if [ $((attempt % 10)) -eq 0 ]; then
    echo "  ...still waiting for the analytics window to close (${attempt}/32)"
  fi
  attempt=$((attempt + 1))
done
if [ -z "$history" ] || [ "$history" = "[]" ]; then
  echo "FAIL: analytics history is empty after ingest and retries." >&2
  exit 1
fi

echo ""
echo "Alerts:"
curl -s "$GATEWAY_URL/api/v1/alerts?limit=10"
echo ""
echo "Analytics live summary:"
curl -s "$GATEWAY_URL/api/v1/analytics/live/summary"
echo ""
echo "Analytics live by type:"
curl -s "$GATEWAY_URL/api/v1/analytics/live/by-type"
echo ""
to=$(date -u +%Y-%m-%dT%H:%M:%SZ)
from=$(date -u -d "-5 minutes" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-5M +%Y-%m-%dT%H:%M:%SZ)
echo ""
echo "Analytics report window (last 5m):"
curl -s "$GATEWAY_URL/api/v1/analytics/report/window?from=$from&to=$to"
echo ""
echo "Smoke-check completed successfully."
