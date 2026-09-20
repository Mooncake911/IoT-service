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
    ├── vm/     # VM под MongoDB/RabbitMQ + backup bucket
    └── k8s/    # Managed Kubernetes кластер
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

### 0. Bootstrap (один раз вручную)

Бакет под стейт не может создать сам себя — до первого `init` его нужно
создать один раз (потом всё идёт автоматом):

```bash
yc storage bucket create --name iot-state-terraform
```

Сама учётка, от имени которой идёт работа, должна иметь роль `editor`
на каталог и доступ к Object Storage, иначе `apply` упадёт с
`PermissionDenied` / S3 `AccessDenied`:

```bash
SA_ID=$(yc iam service-account get --name <sa-name> --format value'(id)')
yc resource-manager folder add-access-binding <folder-id> \
  --role editor --subject serviceAccount:$SA_ID
```

Ключи для S3-бэкенда (`access_key`/`secret_key` в `init -backend-config`)
должны принадлежать сервисному аккаунту с правами на Object Storage
(роль `storage.admin` на каталог); создать их можно так:

```bash
yc iam access-key create --service-account-name <sa-name>
```

Если `yc` сам отвечает `PermissionDenied` даже на чтение (`vpc network get`,
`folder list-access-bindings`) — дело не в Terraform, а в профиле: сверь,
куда вообще смотрит CLI и что тебе видно:

```bash
yc config list
yc resource-manager folder list
```

`folder-id` в выводе должен совпадать с каталогом из `terraform.tfvars`.
Если нужного каталога нет в списке — доступ тебе должен выдать
владелец/админ каталога (роль `editor` на твоего пользователя или на
сервисный аккаунт), после этого повторить `apply`. Несозданные ресурсы
импортировать не нужно; локальный `errored.tfstate` от упавших прогонов
можно удалить.

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

Деплой VM:

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

Деплой K8s:

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
2 CPU / 4 GB), SA с ролями `k8s.clusters.agent`,
`container-registry.images.puller` и `editor` (последняя шире необходимого —
кандидат на урезание до точечных ролей).

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

Плейбук поднимает MongoDB/RabbitMQ на VM (`--profile db`), создаёт
Secret `iot-db` с endpoints БД (коммиченный `k8s/configmap.yaml` не
мутирует), готовит всё, чем ArgoCD владеть не должен, и ждёт Ready всех
подов. Workload'ы накатывает ТОЛЬКО ArgoCD из git:
desired-теги лежат в `k8s/overlays/*` (их пишет CI/CD через `kustomize-set-image`),
никакого `kubectl set image` — он боролся бы с selfHeal. Без ArgoCD
(`enable_argocd=false`, режим лабы 3) — плейбук применяет локальное дерево
напрямую (теги уже зафиксированы в git).
`load-test/job.yaml` специально не применяется автоматически —
только вручную после Ready. С `-e "enable_observability=true"` дополнительно
синкается `k8s/observability/` (Prometheus, Grafana, ELK, Fluent Bit).
Metrics-server в репо не вендорится: плейбук проверяет API
`metrics.k8s.io` и падает с подсказкой (minikube:
`minikube addons enable metrics-server`).

## Переключение VM ↔ K8s

Активный бэкенд в каждый момент только один, иначе будет двойная запись.

VM → K8s гасится явно: `cd-k8s.yml` шагом `vm-down.sh` останавливает
compose-приложения на VM (БД в `db`-профиле не трогает, волюмы целы),
затем поднимает кластерную часть. Версия по умолчанию берётся из
git-пина (`newTag` в оверлеях) — та же, что поедет в k8s; явный `tag`
в форме означает осознанное расхождение версий между режимами.

K8s → VM гасится само в CI: `cd-vm.yml` первым шагом отвязывает кластер
от GitOps (удаляет Applications — иначе selfHeal восстановит снесённое),
затем `k8s-down.sh` удаляет workload'ы и HPA из namespace `iot`
(мониторинг, namespace и ConfigMap остаются) и ждёт их терминации;
затем идёт smoke-чек гейтвея. Если кластера нет (первый VM-деплой) —
шаг пропускается, это не ошибка.
Параллельные запуски CD сериализованы (`concurrency: cd-vm/cd-k8s`),
т.к. S3-бэкенд без локинга.

> При переезде сбрасываются Prometheus (хранилище `emptyDir`) и история
> HPA — графики начинаются с нуля. Для лаб это нормально (отрастают за
> минуты, алерты и дашборды целы), для прода лечится внешним хранилищем
> метрик (remote-write в Thanos/Cortex/Mimir или Managed Prometheus).

Вручную перед ручным `deploy-vm.yml`:

```bash
./scripts/k8s-down.sh
```

## CI/CD

Связь outputs Terraform → переменные CD (`cd-vm.yml` / `cd-k8s.yml` / `cd-destroy.yml`):

| Terraform output | GitHub var |
|---|---|
| `terraform output -raw vm_public_ip` | `VM_PUBLIC_IP` |
| `terraform output -raw vm_internal_ip` | `VM_INTERNAL_IP` |
| `terraform output -raw k8s_cluster_name` | `K8S_CLUSTER_NAME` |

Workflows: `ci.yml` — build/test/push образов в GHCR + bot-коммит тегов
в `k8s/overlays/*` (`pin-images`, `[skip ci]`);
`cd-vm.yml` / `cd-k8s.yml` — деплой по `workflow_dispatch`
(VM: compose с rollback через `.previous_version`;
K8s: ArgoCD-only синк из git, откат — повторный запуск со старым
тегом или `git revert` коммита тегов);
`cd-destroy.yml` — снос `vm`/`k8s` по подтверждению `DESTROY`
(для VM сначала обязательный бэкап Mongo в S3; сеть `base` не трогается).

## Остановка и снос

- Пауза k8s-мира: `./scripts/k8s-down.sh` (сначала отвязывает ArgoCD
Applications, затем сносит деплойменты/HPA/Secret; идемпотентен).
- Пауза VM-мира (волюмы с данными целы): `VM_IP=<ip> ./scripts/vm-down.sh`.
- Полный снос: workflow `cd-destroy.yml`, target `vm` или `k8s`,
подтверждение строкой `DESTROY`. Порядок при зачистке всего —
сначала `k8s`, потом `vm` (иначе кластер останется без БД).
