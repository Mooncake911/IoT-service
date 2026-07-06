# Terraform — Yandex Cloud Infrastructure

Три независимых root-конфига. Каждый можно деплоить отдельно.

```
infra/terraform/
├── base/     # VPC + subnet (обязателен)
├── vm/       # VM для MongoDB + RabbitMQ + backup bucket — Lab 2
└── k8s/      # Managed Kubernetes кластер — Lab 3
```

## Prerequisites

- [Yandex Cloud CLI](https://yandex.cloud/ru/docs/cli/) (`yc`)
- Terraform >= 1.5
- Service account key (JSON) с правами `editor` на каталог
- S3 access keys для state backend (см. `versions.tf` в каждом конфиге)

## Порядок деплоя

### 1. base (сеть)

```bash
cd infra/terraform/base
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"

# Запомнить output'ы (нужны vm и k8s получают их через remote state)
terraform output network_id
terraform output subnet_id
```

### 2. vm (Lab 2)

```bash
cd infra/terraform/vm
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"

# Для Ansible inventory
terraform output vm_public_ip
terraform output vm_internal_ip   # для K8s ConfigMap

# Для Ansible backup credentials
terraform output backup_storage_access_key
terraform output backup_storage_secret_key
```

### 3. k8s (Lab 3)

```bash
cd infra/terraform/k8s
terraform init -backend-config="access_key=<key-id>" -backend-config="secret_key=<secret>"
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"

# Получить kubeconfig
yc managed-kubernetes cluster get-credentials \
  --name $(terraform output -raw k8s_cluster_name) \
  --external

# Проверить
kubectl get nodes
```

## Что создаётся

### base/
| Ресурс | Описание |
|---|---|
| `yandex_vpc_network.iot` | Сеть `iot-network` |
| `yandex_vpc_subnet.iot` | Подсеть `10.10.0.0/24` |

### vm/
| Ресурс | Описание |
|---|---|
| `yandex_compute_instance.iot_vm` | VM с Docker (статический IP `10.10.0.100`) |
| `yandex_vpc_security_group.iot` | Security group с портами сервисов |
| `yandex_iam_service_account.storage_sa` | SA для S3 backup bucket |
| `yandex_storage_bucket.backup_bucket` | Шифрованный S3 bucket для MongoDB backup |

### k8s/
| Ресурс | Описание |
|---|---|
| `yandex_iam_service_account.k8s_sa` | SA (роли: `k8s.clusters.agent`, `container-registry.images.puller`) |
| `yandex_vpc_security_group.k8s_node_sg` | Security group для нод |
| `yandex_kubernetes_cluster.iot` | Managed K8s кластер (1 master) |
| `yandex_kubernetes_node_group.default` | 2 worker-ноды (2 CPU, 4 GB RAM) |

## Переменные

### base/
| Переменная | Дефолт | Описание |
|---|---|---|
| `zone` | `ru-central1-a` | Зона |
| `network_name` | `iot-network` | Имя сети |
| `subnet_cidr` | `10.10.0.0/24` | CIDR подсети |

### vm/
| Переменная | Дефолт | Описание |
|---|---|---|
| `vm_internal_ip` | `10.10.0.100` | Статический IP VM |
| `cores` | `4` | CPU |
| `memory` | `8` | RAM (GB) |
| `backup_bucket_name` | `iot-cold-storage` | Имя S3 bucket |

### k8s/
| Переменная | Дефолт    | Описание |
|---|-----------|---|
| `k8s_cluster_name` | `iot-k8s` | Имя кластера |
| `k8s_version` | `1.35`    | Версия K8s |
| `k8s_node_count` | `2`       | Количество нод |
| `k8s_node_cores` | `2`       | CPU на ноду |
| `k8s_node_memory` | `4`       | RAM (GB) на ноду |

## Удаление

```bash
# Сначала K8s (ноды могут заблокировать subnet)
cd infra/terraform/k8s && terraform destroy

# Потом VM
cd infra/terraform/vm && terraform destroy

# Потом сеть (когда никто не использует)
cd infra/terraform/base && terraform destroy
```

## CI/CD

Output'ы Terraform → GitHub Actions vars:

| Output | GitHub Var | Workflow |
|---|---|---|
| `terraform output -raw vm_public_ip` | `VM_PUBLIC_IP` | `cd.yml` |
| `terraform output -raw vm_internal_ip` | `VM_INTERNAL_IP` | `cd.yml` |
| `terraform output -raw k8s_cluster_name` | `K8S_CLUSTER_NAME` | `cd.yml` |

Workflows:
- [`ci.yml`](/.github/workflows/ci.yml) — push → Maven build + test + Docker push в GHCR
- [`cd.yml`](/.github/workflows/cd.yml) — workflow_dispatch → Terraform (опционально) → Ansible/kubectl
