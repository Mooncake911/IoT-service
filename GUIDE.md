# Руководство по проверке IoT-системы

Актуально для сервисов: `iot-data-simulator`, `iot-controller`, `iot-analytics`, `iot-alerts`, `iot-data-gateway`, `iot-dashboard`.

Все команды — из WSL, из корня репозитория.

## 1. Режимы запуска (Docker Compose)

### Core
```bash
docker compose --profile core up -d --build
docker compose --profile core ps
```

### Core + Observability (ELK + Prometheus + Grafana)
```bash
SPRING_PROFILES="docker,elk" docker compose --profile core --profile observability up -d --build
docker compose --profile core --profile observability ps
```

### Только базы данных (для Kubernetes-режима)
```bash
docker compose --profile db up -d
docker compose --profile db ps
```

Остановка:
```bash
docker compose --profile core down
docker compose --profile core --profile observability down
docker compose --profile db down
```

Полная очистка с volume:
```bash
docker compose down -v
```

## 2. Куда заходить (UI/HTTP, compose-режим)

- Dashboard UI (React/Vite): `http://localhost:8501`
- Gateway: `http://localhost:8085`
- Simulator: `http://localhost:8081`
- Controller: `http://localhost:8082`
- Analytics: `http://localhost:8083`
- Alerts: `http://localhost:8084`
- RabbitMQ Management UI: `http://localhost:15672`
- Prometheus (observability): `http://localhost:9090`
- Grafana (observability): `http://localhost:3000`
- Kibana (observability): `http://localhost:5601`

## 3. RabbitMQ: как смотреть очереди

Через веб-интерфейс: `http://localhost:15672`, логин/пароль из `.env`
(`RABBIT_USER` / `RABBIT_PASS`, обычно `guest/guest`), раздел
`Queues and Streams`: `Ready` — ждут обработки, `Unacked` — доставлены
без подтверждения, `Total` — размер очереди.

```bash
docker exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
docker exec rabbitmq rabbitmq-diagnostics -q check_running
```

Очереди: `iot-analytics.queue`, `iot-alerts.queue` на fanout exchange
`iot.data.exchange`.

## 4. Базовый E2E сценарий (через gateway)

```bash
GW=http://localhost:8085/api/v1

curl -s -X POST "$GW/analytics/config?method=Parallel&windowSeconds=50"
curl -s "$GW/analytics/status"
curl -s -X POST "$GW/simulator/config?deviceCount=10&frequencySeconds=1"
curl -s -X POST "$GW/simulator/start"
curl -s "$GW/simulator/status"

curl -s "$GW/analytics/history?limit=20"
curl -s "$GW/analytics/live/summary"
curl -s "$GW/analytics/live/by-type"
curl -s "$GW/analytics/live/by-manufacturer"
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
FROM=$(date -u -d "10 minutes ago" +%Y-%m-%dT%H:%M:%SZ)
curl -s "$GW/analytics/report/window?from=$FROM&to=$TO"

curl -s "$GW/alerts?limit=20"
```

CRUD правил alerts:

```bash
CREATED=$(curl -s -X POST "$GW/alerts/rules" -H "Content-Type: application/json" -d '{
  "name": "low-battery-cooldown",
  "type": "DURATION",
  "severity": "WARNING",
  "field": "BATTERY_LEVEL",
  "operator": "LT",
  "thresholdNumber": 20,
  "requiredPackets": 3,
  "cooldownSeconds": 30,
  "enabled": true
}')
echo "$CREATED"
curl -s "$GW/alerts/rules"
ID=$(echo "$CREATED" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")
curl -s -X DELETE "$GW/alerts/rules/$ID" -w " HTTP:%{http_code}\n"
```

Остановка simulator:

```bash
curl -s -X POST "$GW/simulator/stop"
```

## 5. Проверка controller через gateway

Внешний путь `POST /api/v1/controller`, внутренний — `POST /api/ingest`
(принимает массив `DeviceData`, отвечает `202`):

```bash
curl -s -X POST http://localhost:8085/api/v1/controller \
  -H "Content-Type: application/json" \
  -d '[{"id":42,"name":"device-42","manufacturer":"acme","type":"SENSOR_TEMPERATURE","capabilities":["temp"],"location":{"x":1,"y":2,"z":0},"status":{"isOnline":true,"batteryLevel":15,"signalStrength":5,"lastHeartbeat":"2026-01-12T12:00:00Z"}}]' \
  -w "\nHTTP:%{http_code}\n"
```

## 6. Базы данных (compose-режим)

- Mongo controller: `localhost:27017`
- Mongo analytics: `localhost:27018`
- Mongo alerts: `localhost:27019`

## 7. Метрики и логи (observability)

- Prometheus targets: `http://localhost:9090/targets`
- Java метрики: `/actuator/prometheus`
- RabbitMQ метрики: `http://localhost:15692/metrics`
- Kibana индекс логов: `logs-*`
- Elasticsearch health: `http://localhost:9200/_cluster/health`

## 8. Kubernetes (minikube под Windows, управление из WSL)

Minikube стартует на Windows (`minikube start`), дальше всё из WSL.
Kubeconfig достать из контейнера minikube и подменить сервер на
проброшенный порт (узнать через `docker ps`, `127.0.0.1:<port>->8443`):

```bash
docker exec minikube cat /etc/kubernetes/admin.conf \
  | sed 's|server: https://.*:8443|server: https://127.0.0.1:<port>|' \
  > ~/.kube/minikube-wsl.conf
export KUBECONFIG=~/.kube/minikube-wsl.conf
kubectl get nodes
```

БД поднимаются на хосте, поды ходят на хост через `host.minikube.internal`:

```bash
docker compose --profile db up -d
```

`k8s/configmap.yaml` содержит только несекретный конфиг; endpoints БД
и credentials живут в Secret `iot-db`, который создаётся на деплое
и никогда не коммитится. Создать вручную:

```bash
VM_INTERNAL_IP=<ip> ./scripts/update-k8s-ips.sh
MINIKUBE=1 ./scripts/update-k8s-ips.sh
```

Ворклоады — Kustomize (`k8s/base/` — единый шаблон деплоймента,
сервиса и HPA; `k8s/overlays/*` — имена/образы/патчи на сервис).
Деплой (порядок важен, `k8s/load-test/` применяется только вручную):

```bash
kubectl apply -k k8s/
kubectl -n iot create secret generic iot-db \
  --from-literal=MONGO_CONTROLLER_URI='mongodb://admin:admin@<db-host>:27017/iot_db_controller?authSource=admin' \
  ... # или скриптом выше
kubectl apply -k k8s/observability/
kubectl get pods -n iot
```

Metrics-server не вендорится — используется кластерный
(minikube: `minikube addons enable metrics-server`).

Доступ через port-forward (gateway NodePort `30085`, grafana NodePort `31300`):

```bash
kubectl port-forward -n iot svc/iot-data-gateway 18085:8080 &
kubectl port-forward -n iot svc/prometheus 19090:9090 &
kubectl port-forward -n iot svc/grafana 13000:3000 &
```

Дальше разделы 4–5 с `GW=http://localhost:18085/api/v1`.
Prometheus targets: `http://localhost:19090/api/v1/targets` (скрап
по-подово через аннотации `prometheus.io/scrape`, лейблы `app`/`namespace`).
Grafana health: `http://localhost:13000/api/health`.

HPA (`k8s/base/hpa/hpa.yaml` via overlays): `iot-controller`, `iot-analytics`, `iot-alerts`,
`cpu averageUtilization: 15%`, `min 1 / max 3`:

```bash
kubectl get hpa -n iot
kubectl top pods -n iot
```

Нагрузочный тест — Job с k6 (валидный массив `DeviceData` в
`POST /api/v1/controller`, 50 VU / 5 мин), только после Ready всех подов:

```bash
kubectl apply -f k8s/load-test/job.yaml
kubectl get hpa -n iot -w
```

На одной ноде minikube полный прогон k6 может упереться в память —
`maxReplicas: 3` и лимиты `768Mi` подобраны под это; в облаке (2 ноды)
запас больше.

Снос workload'ов перед переходом обратно на VM (мониторинг, namespace
и ConfigMap остаются):

```bash
./scripts/k8s-down.sh
```

## 9. Диагностика

```bash
docker compose logs -f iot-controller
docker compose --profile core --profile observability logs -f logstash
docker logs <container_name>
kubectl get pods -n iot -o wide
kubectl describe pod -n iot <pod>
kubectl logs -n iot deploy/iot-controller --tail=50
kubectl get events -n iot --sort-by=.lastTimestamp | tail -n 20
```

Быстрая проверка ELK (compose observability):

```bash
curl -s http://localhost:9200/_cluster/health
curl -s http://localhost:5601/api/status -H "kbn-xsrf: true"
```

## 10. Smoke-check

```bash
./scripts/smoke-check.sh [gateway-url] [dashboard-url]
# Windows: powershell -ExecutionPolicy Bypass -File ./scripts/smoke-check.ps1
```

Валидирует ingest/history/alerts и ручки `live/summary`, `live/by-type`,
`live/by-manufacturer`, `report/window`. CD вызывает его автоматически
после VM- и K8s-деплоя.

## 11. Лаба 4: SonarQube, ArgoCD, Telegram-бот

SonarQube (`ci.yml` jobs `sonar` + `sonar-dashboard`): задать secrets репозитория
`SONAR_TOKEN`, `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY` (бэкенд) и
`SONAR_PROJECT_KEY_DASHBOARD` (клиент). Без токена job'ы пропускаются, но JaCoCo-gate 80%
(`mvn verify` в `test-server`) действует всегда — CI красный при
покрытии ниже порога.

ArgoCD (K8s, opt-in): `cd.yml` → `enable_argocd=true` ставит ArgoCD core
и применяет `k8s/argocd/*.yaml` (GitOps Applications на этот репозиторий,
автосинк prune+selfHeal). Прямой `kubectl apply -k` остаётся bootstrap-путём.
На форке поменять `spec.source.repoURL` в Applications.

Telegram-бот: создать бота через `@BotFather`, задать secrets
`TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID`. CI шлёт итог (`notify` job),
CD — итог деплоя. Без secrets шаги молча скипаются.

## 12. Секреты: единая точка — `.env`

Все креды (`MONGO_USER/PASS`, `RABBIT_USER/PASS`, `GRAFANA_ADMIN_*`)
живут только в корневом `.env`:
- VM/compose-путь читает его нативно;
- k8s-путь читает тот же файл: `deploy-k8s.yml` (задача `Load credentials
from repo .env`) и `scripts/update-k8s-ips.sh` (`ENV_FILE`, явные env
переменные имеют приоритет). В git секреты не попадают — в кластер
едет только Secret `iot-db` / `grafana-admin`.

Поменял пароль в `.env` — пересоздай секреты (деплой или скрипт),
перезапускать ничего вручную не надо: поды подхватят при рестарте.

Миграция на нормальный менеджер (Vault / External Secrets / SOPS):
достаточно заменить один источник — задачу чтения `.env` в плейбуке
и `ENV_FILE` в скрипте — потребители (`dot.*` факты, Secret'ы) не меняются.

## 11. Инфраструктура (Terraform + Ansible)

Общий гайд: [`infra/GUIDE.md`](infra/GUIDE.md) — всё тоже запускается из WSL.
