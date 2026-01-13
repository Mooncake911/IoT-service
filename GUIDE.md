# Руководство по проверке работоспособности IoT системы

Данный документ содержит пошаговую инструкцию по запуску и проверке всех компонентов системы в среде Windows.

---

## 1. Подготовка и запуск
Убедитесь, что у вас установлен Docker Desktop.

1. Откройте терминал (PowerShell или CMD) в корне проекта.
2. Запустите систему:
   ```powershell
   docker-compose up -d --build
   ```
3. Проверьте, что все контейнеры поднялись:
   ```powershell
   docker ps
   ```
   
   **Важно:** Если какой-то контейнер не запустился (статус `Exited`), проверьте логи:
   ```powershell
   docker logs <имя-контейнера>
   ```
   
   Например, для симулятора:
   ```powershell
   docker logs iot-data-simulator
   ```

---

## 2. Использование Симулятора (iot-data-simulator)
Симулятор позволяет генерировать поток данных автоматически.

### 2.1 Настройка параметров
Установим 10 устройств и 1 сообщение в секунду:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/simulator/config?deviceCount=10&messagesPerSecond=1" -Method Post
```

### 2.2 Запуск симуляции
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/simulator/start" -Method Post
```

### 2.3 Проверка статуса
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/simulator/status" -Method Get
```

### 2.4 Остановка симуляции
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/simulator/stop" -Method Post
```

---

## 3. Ручная отправка данных (iot-controller)
Если вы хотите проверить обработку конкретного пакета данных вручную.

```powershell
$json = '[{"id": 42, "status": {"batteryLevel": 15, "signalStrength": 5, "online": true}, "timestamp": "2026-01-12T12:00:00Z"}]'
Invoke-RestMethod -Uri "http://localhost:8082/api/ingest" -Method Post -Body $json -ContentType "application/json"
```

---

## 4. Настройка Аналитики (iot-analytics)
Сервис аналитики агрегирует данные и считает статистику.

### 4.1 Изменение метода расчета
Доступные методы: `Sequential`, `Flowable`, `Observable`, `CustomCollector` и др.
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/analytics/config?method=Flowable&batchSize=20" -Method Post
```

### 4.2 Проверка текущей конфигурации
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/analytics/status" -Method Get
```

---

## 5. Проверка результатов

### 5.1 MongoDB (Хранение данных и алертов)
Используйте **MongoDB Compass** или командную строку для проверки коллекций в базе `iot_db`:
- `devices` — входящие пакеты.
- `alerts` — сработавшие правила от **Rule Engine**.
- `analytics` — результаты работы **Analytics Service**.

### 5.2 Rule Engine (Проверка правил)
Чтобы проверить срабатывание правил:
1. **Мгновенное правило (Low Battery):** Отправьте данные с `batteryLevel < 20` (см. пункт 3). В коллекции `alerts` должен появиться документ с `ruleId: "LOW_BATTERY"`.
2. **Длящееся правило (Sustained Low Signal):** Запустите симулятор с низким сигналом или отправьте 10 пакетов подряд с `signalStrength < 30`. В коллекции `alerts` появится `SUSTAINED_LOW_SIGNAL`.

### 5.3 ELK Stack (Логи)
1. Откройте **Kibana**: [http://localhost:5601](http://localhost:5601)
2. Перейдите в **Stack Management** -> **Data Views**.
3. Создайте паттерн `logs-*`.
4. Перейдите в **Discover** для просмотра логов в реальном времени.

### 5.4 Мониторинг (Метрики)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (Login: `admin` / `admin`).
- **Prometheus**: [http://localhost:9090](http://localhost:9090).

---

## 6. Диагностика проблем

### 6.1 Контейнер не запускается
Если контейнер завершается с ошибкой (`Exited (1)`), проверьте логи:
```powershell
docker logs <имя-контейнера>
```

### 6.2 Сервис недоступен по порту
1. Убедитесь, что контейнер запущен: `docker ps`
2. Проверьте, что порт не занят другим процессом
3. Проверьте логи контейнера на наличие ошибок запуска
4. Убедитесь, что переменные окружения установлены корректно (если используется `.env` файл)

### 6.3 Пересборка конкретного сервиса
Если нужно пересобрать только один сервис:
```powershell
docker-compose up -d --build <имя-сервиса>
```

Например, для симулятора:
```powershell
docker-compose up -d --build iot-data-simulator
```

---

## 7. Полезные команды Docker (Windows)
- Посмотреть последние 50 строк логов сервиса:
  ```powershell
  docker logs --tail 50 iot-controller
  ```
- Посмотреть все контейнеры (включая остановленные):
  ```powershell
  docker ps -a
  ```
- Остановить и удалить всё:
  ```powershell
  docker-compose down -v
  ```
- Перезапустить конкретный сервис:
  ```powershell
  docker-compose restart iot-data-simulator
  ```
