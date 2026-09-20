#!/bin/sh
# Stops IoT app containers on the VM, keeps data volumes intact.
# Companion of k8s-down.sh (use that one for the cluster side).
# Full infra wipe (VM/cluster removal) is a terraform destroy operation,
# see cd-destroy.yml — this script only stops workloads.
set -eu

VM_IP="${VM_IP:?Set VM_IP to the target VM public IP}"
SSH_USER="${SSH_USER:-ubuntu}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
DEPLOY_DIR="${DEPLOY_DIR:-/home/${SSH_USER}/iot-service}"
SSH_OPTS="${SSH_OPTS:- -o StrictHostKeyChecking=no -o ConnectTimeout=15}"

# shellcheck disable=SC2086
ssh $SSH_OPTS -i "$SSH_KEY" "${SSH_USER}@${VM_IP}" \
  "cd '$DEPLOY_DIR' && docker compose --profile core --profile observability down --remove-orphans"

echo "VM app workloads stopped on ${VM_IP} (volumes preserved)."
