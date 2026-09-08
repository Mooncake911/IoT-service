#!/bin/sh
set -eu

CONFIGMAP="${1:-k8s/configmap.yaml}"

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
  NETWORK="iot-service_iot-network"
  get_ip() {
    docker inspect "$1" --format "{{.NetworkSettings.Networks.${NETWORK}.IPAddress}}" 2>/dev/null
  }

  MONGO_CTRL=$(get_ip mongodb-controller)
  MONGO_ANALYTICS=$(get_ip mongodb-analytics)
  MONGO_ALERTS=$(get_ip mongodb-alerts)
  RABBIT=$(get_ip rabbitmq)

  if [ -z "$MONGO_CTRL" ] || [ -z "$RABBIT" ]; then
    echo "ERROR: Cannot detect container IPs. Is docker compose running?"
    exit 1
  fi
fi

echo "Detected IPs:"
echo "  mongodb-controller: $MONGO_CTRL"
echo "  mongodb-analytics:  $MONGO_ANALYTICS"
echo "  mongodb-alerts:     $MONGO_ALERTS"
echo "  rabbitmq:           $RABBIT"

sed -i \
  -e "s|mongodb://admin:admin@[^/:]*:27017/iot_db_controller|mongodb://admin:admin@${MONGO_CTRL}:27017/iot_db_controller|" \
  -e "s|mongodb://admin:admin@[^/:]*:27018/iot_analytics_db|mongodb://admin:admin@${MONGO_ANALYTICS}:27018/iot_analytics_db|" \
  -e "s|mongodb://admin:admin@[^/:]*:27019/iot_alerts_db|mongodb://admin:admin@${MONGO_ALERTS}:27019/iot_alerts_db|" \
  -e "s/RABBIT_HOST: .*/RABBIT_HOST: ${RABBIT}/" \
  "$CONFIGMAP"

echo "Updated $CONFIGMAP"
