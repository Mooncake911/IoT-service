#!/bin/sh
set -eu

NAMESPACE="${NAMESPACE:-iot}"
TIMEOUT="${TIMEOUT:-240s}"

DEPLOYS="iot-controller iot-analytics iot-alerts iot-data-gateway iot-data-simulator iot-dashboard-ui"
HPAS="iot-controller-hpa iot-analytics-hpa iot-alerts-hpa"

command -v kubectl >/dev/null 2>&1 || {
  echo "ERROR: kubectl not found." >&2
  exit 1
}

kubectl cluster-info >/dev/null 2>&1 || {
  echo "No cluster reachable, nothing to tear down."
  exit 0
}

kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || {
  echo "Namespace $NAMESPACE not found, nothing to tear down."
  exit 0
}

TARGETS=""
for d in $DEPLOYS; do
  TARGETS="$TARGETS deploy/$d"
done
for h in $HPAS; do
  TARGETS="$TARGETS hpa/$h"
done

# shellcheck disable=SC2086
kubectl delete -n "$NAMESPACE" --ignore-not-found $TARGETS

# shellcheck disable=SC2086
kubectl wait --for=delete -n "$NAMESPACE" $TARGETS --timeout="$TIMEOUT"

echo "K8s app workloads removed from namespace $NAMESPACE."
