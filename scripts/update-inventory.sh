#!/bin/sh
set -eu

INVENTORY="${1:-infra/ansible/inventory.ini}"
SSH_USER="${SSH_USER:-ubuntu}"
SSH_KEY="${SSH_KEY:-~/.ssh/id_ed25519}"

if [ -n "${VM_IP:-}" ]; then
  IP="$VM_IP"
else
  IP=$(terraform -chdir=infra/terraform/vm output -raw vm_public_ip)
fi

if [ -z "$IP" ]; then
  echo "ERROR: empty VM IP. Set VM_IP or check terraform state." >&2
  exit 1
fi

cat > "$INVENTORY" <<EOF
[iot_vm]
$IP ansible_user=$SSH_USER

[iot_vm:vars]
ansible_ssh_private_key_file=$SSH_KEY
ansible_ssh_common_args='-o StrictHostKeyChecking=no -o ServerAliveInterval=30 -o ServerAliveCountMax=20 -o TCPKeepAlive=yes'
ansible_ssh_timeout=60
ansible_ssh_retries=5
EOF

echo "Wrote $INVENTORY ($IP)"
