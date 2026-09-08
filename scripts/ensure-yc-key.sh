#!/bin/sh
set -eu

KEY_FILE="${1:-/tmp/yc-sa-key.json}"

valid() {
  python3 - "$KEY_FILE" <<'EOF' >/dev/null 2>&1
import json, sys
d = json.load(open(sys.argv[1]))
assert isinstance(d, dict) and d.get("service_account_id") and "PRIVATE KEY" in d.get("private_key", "")
EOF
}

if ! valid; then
  if [ -z "${YC_SA_KEY_JSON:-}" ]; then
    echo "ERROR: no valid key at $KEY_FILE and YC_SA_KEY_JSON is empty." >&2
    exit 1
  fi
  printf '%s' "$YC_SA_KEY_JSON" > "$KEY_FILE"
  chmod 600 "$KEY_FILE"
  if ! valid; then
    echo "ERROR: YC_SA_KEY_JSON is not a valid service account key file." >&2
    exit 1
  fi
fi

SA=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['service_account_id'])" "$KEY_FILE")
echo "SA key OK ($KEY_FILE, service_account_id=$SA)"
