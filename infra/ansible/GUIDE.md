# Ansible

## Установка Docker на VM (первичная настройка)
```bash
ansible-playbook playbooks/install-docker.yml -i inventory.ini
```

## Lab 2 — Deploy to VM (Docker Compose)
```bash
ansible-playbook playbooks/deploy-vm.yml -i inventory.ini \
  -e "github_owner=<your-gh-username>" \
  -e "registry_username=<your-gh-username>" \
  -e "registry_password=<gh-token>" \
  -e "app_version=<tag>"
```

## Lab 3 — Deploy to Managed K8s

Без observability (только core сервисы + БД):
```bash
ansible-playbook playbooks/deploy-k8s.yml -i inventory.ini \
  -e "github_owner=<your-gh-username>" \
  -e "registry_username=<your-gh-username>" \
  -e "registry_password=<gh-token>" \
  -e "app_version=<tag>" \
  -e "vm_internal_ip=<db-vm-ip>"
```

С observability (ELK на VM + Prometheus/Grafana в K8s):
```bash
ansible-playbook playbooks/deploy-k8s.yml -i inventory.ini \
  -e "github_owner=<your-gh-username>" \
  -e "registry_username=<your-gh-username>" \
  -e "registry_password=<gh-token>" \
  -e "app_version=<tag>" \
  -e "vm_internal_ip=<db-vm-ip>" \
  -e "enable_observability=true"
```

## Inventory
```ini
[iot_vm]
<vm-public-ip> ansible_user=ubuntu

[iot_vm:vars]
ansible_ssh_private_key_file=~/.ssh/id_ed25519
```
