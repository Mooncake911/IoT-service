# IoT Service

Репозиторий содержит распределённую IoT-систему на базе Spring WebFlux, Reactor RabbitMQ и MongoDB. Система построена вокруг потока телеметрии от устройств: данные генерируются или поступают извне, проходят через ingestion-сервис, затем асинхронно расходятся в аналитику и подсистему алертов.

Документ ниже описывает не только список сервисов, но и то, как они реально взаимодействуют друг с другом внутри текущей реализации.

## Состав системы

Основные прикладные сервисы:

- `iot-data-simulator` генерирует и обновляет данные устройств, затем отправляет батчи в `iot-controller`.
- `iot-controller` принимает входные данные по HTTP, валидирует их, сохраняет в MongoDB и публикует в RabbitMQ.
- `iot-analytics` читает поток из RabbitMQ, рассчитывает статистику и сохраняет результаты в свою MongoDB.
- `iot-alerts` читает тот же поток из RabbitMQ, применяет правила и сохраняет сработавшие алерты в свою MongoDB.
- `iot-data-gateway` предоставляет единый HTTP-вход для dashboard и проксирует запросы в simulator, controller, analytics и alerts.
- `iot-dashboard` (React/Vite UI-клиент, отдельный submodule) работает поверх gateway и даёт пользовательский интерфейс для управления и просмотра состояния системы.
- `iot-contracts` содержит общие DTO и доменные модели, которые используются несколькими Java-сервисами.

Инфраструктура:

- `rabbitmq` для событийного обмена между сервисами.
- `mongodb-controller`, `mongodb-analytics`, `mongodb-alerts` для раздельного хранения данных по bounded context.
- `elasticsearch`, `logstash`, `kibana` для логов.
- `prometheus`, `grafana` для метрик.

## Высокоуровневая схема взаимодействия

Основной рабочий поток выглядит так:

1. `iot-data-simulator` формирует список `DeviceData` и отправляет его в `iot-controller` по `POST /api/ingest`.
2. `iot-controller` валидирует каждый объект, сохраняет его в `mongodb-controller` и публикует события в fanout exchange `iot.data.exchange` в RabbitMQ.
3. `iot-analytics` и `iot-alerts` подписаны на этот exchange через разные очереди:
   - `iot-analytics.queue`
   - `iot-alerts.queue`
4. `iot-analytics` получает сообщения, собирает окна данных, рассчитывает `AnalyticsData` и сохраняет результат в `mongodb-analytics`.
5. `iot-alerts` получает те же сообщения, вычисляет мгновенные и длительные правила, формирует `AlertData` и сохраняет результат в `mongodb-alerts`.
6. `iot-dashboard` не ходит напрямую в backend-сервисы, а использует `iot-data-gateway` как единую точку входа.
7. `iot-data-gateway` маршрутизирует запросы на:
   - `iot-data-simulator`
   - `iot-controller`
   - `iot-analytics`
   - `iot-alerts`

Идея архитектуры в том, что ingestion отделён от downstream-обработки. Controller подтверждает приём данных после записи и публикации, а аналитика и алерты выполняются асинхронно и независимо друг от друга.

## Детальный разбор по сервисам

### `iot-data-simulator`

Назначение:

- Генерирует набор устройств через `DeviceGenerator`.
- Поддерживает управление числом устройств, частотой генерации и batch size.
- При запуске периодически обновляет состояние устройств и отправляет пачки в controller.

Ключевое взаимодействие:

- Исходящий HTTP-запрос в `iot-controller`.
- URL задаётся через `CONTROLLER_URL`.
- В `docker-compose` simulator отправляет данные в:
  - `http://iot-controller:8080/api/ingest`

HTTP API:

- `POST /api/simulator/config?deviceCount=<n>&frequencySeconds=<n>`
- `POST /api/simulator/start`
- `POST /api/simulator/stop`
- `GET /api/simulator/status`

Поведение внутри:

- Сервис хранит текущие настройки в `AtomicInteger`.
- После `start()` включает reactive pipeline на `Flux.interval(...)`.
- Для каждого тика берёт часть списка устройств, обновляет их и отправляет батч в controller через `WebClient`.
- Ошибки отправки логируются и гасятся, чтобы pipeline не останавливался полностью.

### `iot-controller`

Назначение:

- Главная ingress-точка для телеметрии.
- Валидирует входящие данные.
- Сохраняет устройства в MongoDB.
- Публикует события в RabbitMQ для downstream-сервисов.

HTTP API:

- `POST /api/ingest`

Формат входа:

- Принимает `List<DeviceData>`.
- Каждый элемент содержит:
  - `id`
  - `name`
  - `manufacturer`
  - `type`
  - `capabilities`
  - `location`
  - `status`

Внутренний pipeline:

1. `IngestionController` принимает batch.
2. `IngestionService` валидирует каждый `DeviceData` через `DeviceValidator`.
3. Данные конвертируются в `DeviceEntity`.
4. Запись выполняется в `mongodb-controller`.
5. После записи каждое устройство сериализуется и публикуется в RabbitMQ через `Sender`.

RabbitMQ-часть:

- Exchange: `iot.data.exchange`
- Тип exchange: `fanout`
- Routing key фактически не используется, так как exchange широковещательный.
- Публикация выполняется реактивно и чанкуется по `app.rabbitmq.chunk-size`.

Это означает, что downstream-сервисы получают идентичный поток событий, но обрабатывают его независимо и с собственной скоростью.

### `iot-analytics`

Назначение:

- Считает real-time аналитику по входящему потоку устройств из RabbitMQ.
- Хранит агрегированные окна в MongoDB.
- Отдаёт классическую отчётную аналитику по временному окну из MongoDB.

Источники данных:

- Основной production-поток приходит из RabbitMQ.
- Также есть прямой HTTP endpoint для ручного расчёта/отладки.

HTTP API:

- `POST /api/analytics/data`
- `POST /api/analytics/config?method=<name>&windowSeconds=<n>`
- `GET /api/analytics/status`
- `GET /api/analytics/history?limit=<n>`
- `GET /api/analytics/live/summary`
- `GET /api/analytics/live/by-type`
- `GET /api/analytics/live/by-manufacturer`
- `GET /api/analytics/report/window?from=<iso>&to=<iso>`

Поддерживаемые значения `method`:
- `Parallel` (по умолчанию)
- `Sequential`

Как работает consumption из RabbitMQ:

1. Сервис подписывается на очередь `iot-analytics.queue`.
2. `AmqpConsumer` читает delivery через `consumeManualAck(...)` и десериализует `DeviceData`.
3. Устройства агрегируются в общее окно `bufferTimeout(...)` по всему входящему потоку.
4. Для окна одновременно:
   - обновляется live-состояние (`LiveAnalyticsService`);
   - считается агрегат `AnalyticsData` и сохраняется в `mongodb-analytics`.
5. Delivery подтверждаются (`ack`) после извлечения/десериализации и отправки в поток обработки.

Настройка расчётов:

- Текущий метод и длительность окна хранятся в `AnalyticsService`.
- Изменение конфигурации через `/api/analytics/config` меняет метод расчёта и длительность временного окна, по которому consumer строит агрегаты.

### `iot-alerts`

Назначение:

- Вычисляет алерты по данным устройств.
- Поддерживает мгновенные правила и правила длительности.
- Хранит историю срабатываний в MongoDB.

HTTP API:

- `GET /api/alerts?limit=<n>`
- `GET /api/alerts/rules`
- `POST /api/alerts/rules`
- `PUT /api/alerts/rules/{id}`
- `DELETE /api/alerts/rules/{id}`

Поддержка правил:

- Типы: `INSTANT`, `DURATION`
- Поля: `BATTERY_LEVEL`, `SIGNAL_STRENGTH`, `DEVICE_NAME`, `MANUFACTURER`, `DEVICE_TYPE`, `IS_ONLINE`
- Операторы: `LT`, `LTE`, `GT`, `GTE`, `EQ`, `NEQ`, `CONTAINS` (с валидацией совместимости field/operator)
- Для `DURATION`: `requiredPackets`
- Для анти-шума: `cooldownSeconds` (минимальный интервал между повторами алерта для пары device+rule)

Как работает consumption из RabbitMQ:

1. Сервис подписывается на очередь `iot-alerts.queue`.
2. Очередь привязана к тому же `iot.data.exchange`.
3. `AmqpConsumer` получает сообщение и превращает его в список `DeviceData`.
4. Для каждого устройства вызывается `RuleEngine.processDevice(...)`.
5. `RuleEngine`:
   - проверяет instant rules;
   - обновляет `DeviceStateTracker` для duration rules;
   - создаёт `AlertData` только для реально сработавших условий.
6. Готовые алерты батчатся и сохраняются через `AlertPersistence.saveBatch(...)` в `mongodb-alerts`.
7. После этого сообщение подтверждается через `ack`.

Следствие для архитектуры:

- analytics и alerts читают один и тот же поток событий, но никак не зависят друг от друга напрямую;
- сбой или замедление одной ветки не должно ломать вторую, пока RabbitMQ и controller доступны.

### `iot-data-gateway`

Назначение:

- Единая HTTP-точка входа для dashboard и внешних клиентов.
- Не хранит данные и не делает доменную обработку.
- Только маршрутизирует запросы в профильные сервисы.

Маршруты:

- `/api/controller/**` -> `iot-controller` (rewrite в `/api/ingest/**` внутри controller)
- `/api/v1/controller/**` -> `iot-controller` (rewrite в `/api/ingest/**` внутри controller)
- `/api/analytics/**` -> `iot-analytics`
- `/api/alerts/**` -> `iot-alerts`
- `/api/simulator/**` -> `iot-data-simulator`

Особенности:

- Работает на Spring Cloud Gateway Server WebFlux.
- Настроен `Retry` filter для upstream-сервисов.
- Использует `GatewayLoggingFilter`, который логирует входящие запросы и исходящие статусы.

Практический смысл:

- dashboard знает только gateway URL;
- backend-сервисы можно менять за gateway без переписывания клиентской части.

### `iot-dashboard`

Назначение:

- React/Vite UI-клиент для пользовательского взаимодействия.
- Ходит только в `iot-data-gateway`.
- Подключён как отдельный `git submodule`.

Что dashboard делает через gateway:

- читает статус simulator: `/api/simulator/status`
- запускает и останавливает simulator
- меняет конфигурацию simulator
- читает статус analytics
- меняет конфигурацию analytics
- запрашивает историю analytics: `/api/analytics/history`
- запрашивает live-аналитику: `/api/analytics/live/summary`, `/api/analytics/live/by-type`, `/api/analytics/live/by-manufacturer`
- запрашивает report-аналитику по окну: `/api/analytics/report/window`
- запрашивает alerts: `/api/alerts`
- управляет правилами alerts: `/api/alerts/rules`

Прямого доступа к MongoDB или RabbitMQ у dashboard нет.

## Хранение данных

Система использует отдельную базу на каждый сервисный контекст.

### `mongodb-controller`

Хранит ingest-слой:

- последние записанные устройства;
- нормализованное представление входных `DeviceData` как `DeviceEntity`.

### `mongodb-analytics`

Хранит производные аналитические записи:

- рассчитанные `AnalyticsData`;
- историю вычислений, доступную через `/api/analytics/history`.

### `mongodb-alerts`

Хранит сработавшие алерты:

- `AlertData`;
- историю, доступную через `/api/alerts`.

Такое разделение упрощает изоляцию нагрузки и делает ownership данных очевидным: controller не хранит alerts, alerts не хранит raw ingest и так далее.

## RabbitMQ и событийная модель

Текущая схема RabbitMQ:

- exchange:
  - `iot.data.exchange` (`fanout`)
- queues:
  - `iot-analytics.queue`
  - `iot-alerts.queue`

Почему используется `fanout`:

- controller публикует одно событие на устройство;
- analytics и alerts должны получить одно и то же событие одновременно;
- routing key не нужен, потому что здесь не selective routing, а broadcast на всех подписчиков.

Порядок обработки:

1. Controller сохраняет данные.
2. Controller публикует события.
3. Analytics и alerts потребляют сообщения параллельно.
4. Каждая ветка независимо выполняет `ack` только после своей успешной обработки.

Это и есть центральная асинхронная связка всей системы.

## Наблюдаемость и операционная часть

### Логи

Java-сервисы завязаны на Logstash/ELK:

- сервисы стартуют с профилем `elk`;
- логи уходят в `logstash`;
- далее индексируются в `elasticsearch`;
- просматриваются через `kibana`.

Compose-поток:

- `service -> logstash -> elasticsearch -> kibana`

### Метрики

Сервисы экспортируют:

- `/actuator/prometheus`

Prometheus собирает метрики по HTTP, Grafana визуализирует их.

Важно: текущий `monitoring/prometheus/prometheus.yml` уже содержит scrape для `iot-controller` и `iot-analytics`, но в нём всё ещё есть устаревшая цель `iot-rule-engine`, которой в текущей системе нет. Документация здесь описывает фактическую архитектуру, а не старое имя сервиса.

## Реальные URL и порты в Docker Compose

Пользовательские порты на хосте:

- `iot-data-gateway`: `localhost:8085`
- `iot-controller`: `localhost:8082`
- `iot-analytics`: `localhost:8083`
- `iot-alerts`: `localhost:8084`
- `iot-data-simulator`: `localhost:8081`
- `iot-dashboard`: `localhost:8501`
- `rabbitmq`: `localhost:5672`
- `rabbitmq management`: `localhost:15672`
- `mongodb-controller`: `localhost:27017`
- `mongodb-analytics`: `localhost:27018`
- `mongodb-alerts`: `localhost:27019`
- `kibana`: `localhost:5601`
- `elasticsearch`: `localhost:9200`
- `prometheus`: `localhost:9090`
- `grafana`: `localhost:3000`

Внутри docker-сети Java-сервисы слушают единый внутренний порт `8080`, а внешние различия обеспечиваются пробросом портов через compose.

## Примеры рабочих сценариев

### Сценарий 1. Полный путь телеметрии

1. Simulator генерирует новые устройства или обновляет их состояние.
2. Simulator делает `POST` в controller.
3. Controller принимает batch и отвечает `202 Accepted`.
4. Controller сохраняет данные в `mongodb-controller`.
5. Controller публикует события в `iot.data.exchange`.
6. Analytics получает сообщение, рассчитывает агрегаты, пишет их в `mongodb-analytics`.
7. Alerts получает то же сообщение, вычисляет правила, пишет алерты в `mongodb-alerts`.
8. Dashboard читает историю аналитики и алертов через gateway.

### Сценарий 2. Пользователь меняет поведение аналитики

1. Пользователь открывает dashboard.
2. Dashboard отправляет `POST /api/analytics/config?...` в gateway.
3. Gateway проксирует запрос в `iot-analytics`.
4. `AnalyticsService` меняет метод расчёта и длительность временного окна.
5. Новый конфиг начинает влиять на последующие окна consumer-а.

### Сценарий 3. Пользователь управляет генерацией

1. Dashboard делает `POST /api/simulator/config?...`.
2. Gateway проксирует запрос в simulator.
3. Simulator меняет число устройств и частоту.
4. После `POST /api/simulator/start` включается reactive pipeline генерации.

## Быстрый старт

Требования:

- Docker
- Docker Compose

Запуск:

```bash
docker compose up -d --build
```

Проверка, что стек поднялся:

```bash
docker compose ps
```

Smoke-check всего потока:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-check.ps1
```

Скрипт:

- отправляет тестовый batch в `/api/v1/controller` через gateway;
- ждёт завершения асинхронной обработки;
- затем читает:
  - `/api/analytics/history`
  - `/api/analytics/live/summary`
  - `/api/analytics/live/by-type`
  - `/api/analytics/live/by-manufacturer`
  - `/api/analytics/report/window`
  - `/api/alerts`

## Ключевые свойства архитектуры

- Вход в систему синхронный по HTTP, дальнейшая обработка асинхронная через RabbitMQ.
- Analytics и alerts являются независимыми потребителями одного потока событий.
- Данные разделены по MongoDB-инстансам на уровень ingestion, analytics и alerts.
- Dashboard не зависит от внутренней топологии сервисов и работает только через gateway.
- Reactor используется и для HTTP, и для внутренних pipeline обработки, и для RabbitMQ consumption/publication.

## Что важно помнить при развитии системы

- Если меняется структура `DeviceData`, нужно синхронно обновлять `iot-contracts`, simulator, controller, analytics и alerts.
- Если добавляется новый downstream-потребитель событий, ему достаточно подключить собственную очередь к `iot.data.exchange`.
- Если меняется внешний API для dashboard, изменения почти всегда должны идти через `iot-data-gateway`, а не напрямую в UI-клиент.
- Если меняются имена сервисов или портов, документация должна сверяться не со старыми README, а с `docker-compose.yml` и `application.yml` конкретных модулей.
