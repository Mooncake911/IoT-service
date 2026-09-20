#!/bin/sh
# Creates/updates the `iot-db` Secret with DB endpoints for K8s workloads.
# Replaces the old `sed -i k8s/configmap.yaml` approach: the committed
# ConfigMap stays untouched, secrets never land in git.
#
# Modes (same as before):
#   VM_INTERNAL_IP=<ip> ./scripts/update-k8s-ips.sh   # cloud VM
#   MINIKUBE=1 ./scripts/update-k8s-ips.sh             # minikube
#   ./scripts/update-k8s-ips.sh                        # local docker IPs
set -eu

NAMESPACE="${NAMESPACE:-iot}"
SECRET_NAME="${SECRET_NAME:-iot-db}"
NETWORK="${NETWORK:-iot-service_iot-network}"

# .env is the single source of truth for secrets (same file compose uses).
# Explicit env vars take precedence over .env values.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/../.env}"
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi
MONGO_USER="${MONGO_USER:-admin}"
MONGO_PASS="${MONGO_PASS:-admin}"
RABBIT_USER="${RABBIT_USER:-guest}"
RABBIT_PASS="${RABBIT_PASS:-guest}"
echo "Credentials loaded from: ${ENV_FILE} (user=$MONGO_USER/rabbit=$RABBIT_USER)"

if [ -n "${VM_INTERNAL_IP:-}" ]; then
  echo "Cloud mode: using VM_INTERNAL_IP=$VM_INTERNAL_IP"
  MONGO_CTRL="$VM_INTERNAL_IP"
  MONGO_ANALYTICS="$VM_INTERNAL_IP"
  MONGO_ALERTS="$VM_INTERNAL_IP"
  RABBIT="$VM_INTERNAL_IP"
elif [ -n "${MINIKUBE:-}" ]; then
  echo "Minikube mode: using host.minikube.internal"
  MONGO_CTRL="host.minikube.internal"
  MONGO_ANALYTICS="host.minikube.internal"
  MONGO_ALERTS="host.minikube.internal"
  RABBIT="host.minikube.internal"
else
  # Local mode: detect IPs from Docker containers
  get_ip() {
    docker inspect "$1" --format "{{.NetworkSettings.Networks.${NETWORK}.IPAddress}}" 2>/dev/null
  }

  MONGO_CTRL=$(get_ip mongodb-controller)
  MONGO_ANALYTICS=$(get_ip mongodb-analytics)
  MONGO_ALERTS=$(get_ip mongodb-alerts)
  RABBIT=$(get_ip rabbitmq)

  if [ -z "$MONGO_CTRL" ] || [ -z "$RABBIT" ]; then
    echo "ERROR: Cannot detect container IPs. Is docker compose running?" >&2
    exit 1
  fi
fi

echo "Detected IPs:"
echo "  mongodb-controller: $MONGO_CTRL"
echo "  mongodb-analytics:  $MONGO_ANALYTICS"
echo "  mongodb-alerts:     $MONGO_ALERTS"
echo "  rabbitmq:           $RABBIT"

kubectl -n "$NAMESPACE" create secret generic "$SECRET_NAME" \
  --from-literal=MONGO_CONTROLLER_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_CTRL}:27017/iot_db_controller?authSource=admin" \
  --from-literal=MONGO_ANALYTICS_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_ANALYTICS}:27018/iot_analytics_db?authSource=admin" \
  --from-literal=MONGO_ALERTS_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_ALERTS}:27019/iot_alerts_db?authSource=admin" \
  --from-literal=RABBIT_HOST="$RABBIT" \
  --from-literal=RABBIT_USER="$RABBIT_USER" \
  --from-literal=RABBIT_PASS="$RABBIT_PASS" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secret $NAMESPACE/$SECRET_NAME applied"
