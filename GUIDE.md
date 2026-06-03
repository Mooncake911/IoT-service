# Руководство по проверке IoT-системы

Актуально для сервисов: `iot-data-simulator`, `iot-controller`, `iot-analytics`, `iot-alerts`, `iot-data-gateway`, `iot-dashboard`.

## 1. Режимы запуска

### Core (быстрый локальный режим)
```powershell
docker compose up -d --build
docker compose ps
```

### Core + Observability (ELK + Prometheus + Grafana)
```powershell
$env:SPRING_PROFILES="docker,elk"
docker compose --profile observability up -d --build
docker compose --profile observability ps
```

Остановить всё:
```powershell
docker compose down
```

Полная очистка с volume:
```powershell
docker compose down -v
```

## 2. Куда заходить (UI/HTTP)

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

### Через веб-интерфейс
1. Открыть `http://localhost:15672`
2. Логин/пароль: значения из `.env` (`RABBIT_USER` / `RABBIT_PASS`, обычно `guest/guest`)
3. Перейти в раздел `Queues and Streams`
4. Смотреть:
   - `Ready` — ждут обработки
   - `Unacked` — доставлены, но не подтверждены consumer’ом
   - `Total` — суммарный размер очереди

### Через команду из контейнера
```powershell
docker exec -it rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Проверить общую диагностику:
```powershell
docker exec -it rabbitmq rabbitmq-diagnostics -q check_running
```

## 4. Базовый E2E сценарий

Настроить analytics (метод и длительность окна):
```powershell
# method: Sequential | Parallel
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/config?method=Parallel&windowSeconds=50" -Method Post
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/status" -Method Get
```

Настроить simulator:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/simulator/config?deviceCount=10&frequencySeconds=1" -Method Post
```

Запустить simulator:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/simulator/start" -Method Post
```

Проверить статус simulator:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/simulator/status" -Method Get
```

Проверить analytics:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/status" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/history?limit=20" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/live/summary" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/live/by-type" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/live/by-manufacturer" -Method Get
$to = [DateTime]::UtcNow
$from = $to.AddMinutes(-10)
$fromIso = $from.ToString("yyyy-MM-ddTHH:mm:ssZ")
$toIso = $to.ToString("yyyy-MM-ddTHH:mm:ssZ")
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/analytics/report/window?from=$fromIso&to=$toIso" -Method Get
```

Проверить alerts:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/alerts?limit=20" -Method Get
```

Проверить CRUD правил alerts:
```powershell
$rule = @{
  name = "low-battery-cooldown"
  type = "DURATION"
  severity = "WARNING"
  field = "BATTERY_LEVEL"
  operator = "LT"
  thresholdNumber = 20
  requiredPackets = 3
  cooldownSeconds = 30
  enabled = $true
} | ConvertTo-Json

$created = Invoke-RestMethod -Uri "http://localhost:8085/api/v1/alerts/rules" -Method Post -Body $rule -ContentType "application/json"
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/alerts/rules" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/alerts/rules/$($created.id)" -Method Delete
```

Остановить simulator:
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/simulator/stop" -Method Post
```

## 5. Проверка controller через gateway

Внешний путь в gateway: `POST /api/v1/controller`  
Внутренний путь controller: `POST /api/ingest`

```powershell
$json = '[{"id":42,"name":"device-42","manufacturer":"acme","type":"SENSOR_TEMPERATURE","capabilities":["temp"],"location":{"x":1,"y":2,"z":0},"status":{"isOnline":true,"batteryLevel":15,"signalStrength":5,"lastHeartbeat":"2026-01-12T12:00:00Z"}}]'
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/controller" -Method Post -Body $json -ContentType "application/json"
```

## 6. Базы данных

- Mongo controller: `localhost:27017`
- Mongo analytics: `localhost:27018`
- Mongo alerts: `localhost:27019`

## 7. Метрики и логи (observability)

- Prometheus targets: `http://localhost:9090/targets`
- Java метрики: `/actuator/prometheus`
- RabbitMQ метрики: `http://localhost:15692/metrics`
- Gateway API docs links: `http://localhost:8085/api/docs`
- Kibana индекс логов: `logs-*`
- Elasticsearch health: `http://localhost:9200/_cluster/health`

## 8. Диагностика

Логи сервиса:
```powershell
docker compose logs -f iot-controller
```

Логи logstash:
```powershell
docker compose --profile observability logs -f logstash
```

Быстрая проверка ELK:
```powershell
docker compose --profile observability ps
Invoke-RestMethod -Uri "http://localhost:9200/_cluster/health" -Method Get
Invoke-RestMethod -Uri "http://localhost:5601/api/status" -Headers @{ "kbn-xsrf" = "true" } -Method Get
```

Если контейнер не поднялся:
```powershell
docker logs <container_name>
```

## 9. Smoke-check

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-check.ps1
```

Smoke-check теперь валидирует не только ingest/history/alerts, но и новые ручки:
- `/api/v1/analytics/live/summary`
- `/api/v1/analytics/live/by-type`
- `/api/v1/analytics/live/by-manufacturer`
- `/api/v1/analytics/report/window`
