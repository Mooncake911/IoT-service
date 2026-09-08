# Infra — Terraform + Ansible

Всё запускается из WSL. Раскладка:

```
infra/
├── GUIDE.md
├── ansible/
│   ├── ansible.cfg
│   ├── inventory.ini
│   ├── inventory.example.ini
│   └── playbooks/
│       ├── install-docker.yml
│       ├── deploy-vm.yml
│       ├── deploy-k8s.yml
│       ├── deploy-common.yml
│       └── templates/backup.sh.j2
└── terraform/
    ├── terraform.tfvars.example
    ├── terraform.rc.example
    ├── base/   # VPC + subnet
    ├── vm/     # VM под MongoDB/RabbitMQ + backup bucket (Lab 2)
    └── k8s/    # Managed Kubernetes кластер (Lab 3)
```

## Требования

- Terraform >= 1.5
- Ansible
- `yc` (Yandex Cloud CLI)
- `kubectl`
- SSH-ключ (`~/.ssh/id_ed25519`)

Для РФ может понадобиться зеркало провайдеров: скопировать
`terraform.rc.example` в `~/.terraformrc`.

## Terraform

Каждый каталог `base/`, `vm/`, `k8s/` — независимый root-конфиг со своим
S3-бэкендом (`iot-state-terraform`). Порядок строгий: `base` → `vm` → `k8s`
(`vm` и `k8s` тянут сеть через `terraform_remote_state`).

Подготовка переменных (в каждом каталоге):

```bash
cp ../terraform.tfvars.example terraform.tfvars
```

Заполнить: `yc_cloud_id`, `yc_folder_id`, `yc_service_account_key_file`
(SA с ролью `editor`), `ssh_public_key`, остальное по умолчанию.

Деплой сети:

```bash
cd infra/terraform/base
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
terraform output network_id
terraform output subnet_id
```

Деплой VM (Lab 2):

```bash
cd ../vm
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
terraform output vm_public_ip      # подхватит ./scripts/update-inventory.sh
terraform output vm_internal_ip    # в K8s ConfigMap (10.10.0.100 по умолчанию)
terraform output backup_storage_access_key
terraform output backup_storage_secret_key
```

Создаётся: `yandex_compute_instance.iot_vm` (4 CPU / 8 GB),
security group с портами сервисов, SA + шифрованный S3-бакет под бэкапы MongoDB.

Деплой K8s (Lab 3):

```bash
cd ../k8s
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
yc managed-kubernetes cluster get-credentials \
  --name $(terraform output -raw k8s_cluster_name) \
  --external
kubectl get nodes
```

Создаётся: кластер `iot-k8s` (K8s 1.35), нод-группа (по умолчанию 2 ноды
2 CPU / 4 GB), SA с ролями `k8s.clusters.agent` и
`container-registry.images.puller`.

Удаление — в обратном порядке (`k8s` → `vm` → `base`):

```bash
cd infra/terraform/k8s && terraform destroy -var-file="terraform.tfvars"
cd ../vm && terraform destroy -var-file="terraform.tfvars"
cd ../base && terraform destroy -var-file="terraform.tfvars"
```

## Ansible

Инвентарь (`infra/ansible/inventory.ini`, в гите его нет — он в `.gitignore`)
генерируется скриптом, руками IP править не нужно:

```bash
./scripts/update-inventory.sh
```

Скрипт берёт IP из `terraform output -raw vm_public_ip` (нужен
проинициализированный `infra/terraform/vm`). Альтернативно — через env,
минуя state:

```bash
VM_IP=<vm-public-ip> ./scripts/update-inventory.sh
SSH_USER=ubuntu SSH_KEY=~/.ssh/id_ed25519 ./scripts/update-inventory.sh [inventory-file]
```

Заготовка вручную (если скрипт недоступен): скопировать
`infra/ansible/inventory.example.ini` в `inventory.ini` и вписать
публичный IP VM и путь к ключу.

Установка Docker на VM:

```bash
ansible-playbook playbooks/install-docker.yml
```

Ставит `docker-ce`, `compose-plugin`, `buildx`, включает сервис, добавляет
пользователя в группу `docker`, настраивает registry-mirrors.

Lab 2 — деплой на VM (Docker Compose):

```bash
ansible-playbook playbooks/deploy-vm.yml \
  -e "github_owner=<gh-user>" \
  -e "registry_username=<gh-user>" \
  -e "registry_password=<gh-token>" \
  -e "app_version=<tag>"
```

Тянет образы из GHCR и поднимает `--profile core` (с бэкапом MongoDB перед
деплойментом). Опционально `-e "enable_observability=true"` для
`core + observability`.

Lab 3 — БД на VM + приложение в K8s:

```bash
ansible-playbook playbooks/deploy-k8s.yml \
  -e "github_owner=<gh-user>" \
  -e "registry_username=<gh-user>" \
  -e "registry_password=<gh-token>" \
  -e "app_version=<tag>" \
  -e "vm_internal_ip=<db-vm-ip>"
```

Плейбук поднимает MongoDB/RabbitMQ на VM (`--profile db`), патчит
`k8s/configmap.yaml` под IP VM, применяет манифесты (`namespace`,
`configmap`, workload'ы, `hpa.yaml`, `metrics-server`) и ждёт Ready всех
подов. `k8s/load-test/job.yaml` специально не применяется автоматически —
только вручную после Ready. С `-e "enable_observability=true"` дополнительно
применяет `k8s/observability/` (Prometheus, Grafana, ELK, Fluent Bit).

## CI/CD

Связь outputs Terraform → переменные `cd.yml`:

| Terraform output | GitHub var |
|---|---|
| `terraform output -raw vm_public_ip` | `VM_PUBLIC_IP` |
| `terraform output -raw vm_internal_ip` | `VM_INTERNAL_IP` |
| `terraform output -raw k8s_cluster_name` | `K8S_CLUSTER_NAME` |

Workflows: `ci.yml` — build/test/push образов в GHCR;
`cd.yml` — деплой через Ansible/kubectl по `workflow_dispatch`.
