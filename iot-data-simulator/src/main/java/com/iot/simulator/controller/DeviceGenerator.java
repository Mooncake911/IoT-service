package com.iot.simulator.controller;

import com.iot.shared.domain.DeviceData;
import com.iot.shared.domain.components.Location;
import com.iot.shared.domain.components.Status;
import com.iot.shared.domain.components.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

public class DeviceGenerator {
    private static long idCounter = 1;

    private static final Random RANDOM = new Random();

    private static final Map<Type, String[]> DEVICE_NAMES_BY_TYPE = Map.of(
            Type.SENSOR_TEMPERATURE, new String[] {
                    "TempSensor", "ThermoProbe", "ClimateSensor", "HeatDetector",
                    "TempMonitor", "ThermalSensor", "WeatherStation", "TempGauge"
            },
            Type.SENSOR_HUMIDITY, new String[] {
                    "HumiditySensor", "MoistureDetector", "Hygrometer", "HumidityProbe",
                    "WetnessSensor", "DampDetector", "HumidityGauge", "MoistureMonitor"
            },
            Type.ACTUATOR_LIGHT, new String[] {
                    "SmartBulb", "LEDStrip", "DimmerSwitch", "LightPanel",
                    "LampController", "SmartLight", "LightStrip", "BulbController"
            },
            Type.ACTUATOR_LOCK, new String[] {
                    "SmartLock", "DoorLock", "AccessControl", "LockController",
                    "SecurityLock", "KeylessEntry", "DoorActuator", "LockSystem"
            },
            Type.CAMERA, new String[] {
                    "SecurityCam", "IPCamera", "SurveillanceCam", "VideoCamera",
                    "MotionCam", "DoorbellCam", "SecurityEye", "WatchCam"
            },
            Type.SMART_PLUG, new String[] {
                    "SmartOutlet", "PowerPlug", "EnergyMonitor", "SmartSocket",
                    "PowerController", "OutletSwitch", "EnergyPlug", "SmartReceptacle"
            },
            Type.GATEWAY, new String[] {
                    "SmartHub", "GatewayHub", "ControlCenter", "BridgeHub",
                    "NetworkHub", "IoTGateway", "SmartBridge", "ControlHub"
            });

    private static final String[] MANUFACTURERS = {
            "Samsung", "Philips", "Xiaomi", "Yandex", "Siemens", "Schneider",
            "Honeywell", "Bosch", "Legrand", "Lutron", "Eaton", "ABB",
            "Schneider Electric", "Honeywell", "Johnson Controls", "Crestron",
            "Control4", "Savant", "Elan", "RTI", "AMX", "Extron"
    };

    private static final Map<Type, String[]> CAPABILITIES_BY_TYPE = Map.of(
            Type.SENSOR_TEMPERATURE, new String[] {
                    "TemperatureSensor", "WiFi", "Bluetooth", "BatteryPowered",
                    "DataLogging", "Alerts", "Calibration", "WeatherResistant"
            },
            Type.SENSOR_HUMIDITY, new String[] {
                    "HumiditySensor", "WiFi", "Bluetooth", "BatteryPowered",
                    "DataLogging", "Alerts", "Calibration", "WeatherResistant"
            },
            Type.ACTUATOR_LIGHT, new String[] {
                    "WiFi", "Zigbee", "Z-Wave", "Dimmer", "ColorControl",
                    "Scheduling", "VoiceControl", "MotionSensor", "EnergyMonitoring"
            },
            Type.ACTUATOR_LOCK, new String[] {
                    "WiFi", "Zigbee", "Z-Wave", "Keypad", "Fingerprint",
                    "RFID", "MobileApp", "VoiceControl", "AutoLock", "TamperAlarm"
            },
            Type.CAMERA, new String[] {
                    "WiFi", "Ethernet", "NightVision", "MotionDetection",
                    "AudioRecording", "CloudStorage", "MobileApp", "PTZ", "WeatherResistant"
            },
            Type.SMART_PLUG, new String[] {
                    "WiFi", "Zigbee", "Z-Wave", "EnergyMonitoring", "Scheduling",
                    "VoiceControl", "MobileApp", "OverloadProtection", "Timer"
            },
            Type.GATEWAY, new String[] {
                    "WiFi", "Ethernet", "Zigbee", "Z-Wave", "Bluetooth",
                    "CloudConnectivity", "MobileApp", "VoiceControl", "Scheduling", "Automation"
            });

    private static final String[] COMMON_CAPABILITIES = {
            "WiFi", "Bluetooth", "Zigbee", "Z-Wave", "VoiceControl", "MobileApp",
            "Scheduling", "Automation", "EnergyMonitoring", "DataLogging", "Alerts"
    };

    // === Методы для генерации различных распределений ===

    /**
     * Генерирует значение по нормальному распределению (Box-Muller transform)
     */
    private double generateNormal(double mean, double stdDev) {
        if (RANDOM.nextBoolean()) {
            double u1 = RANDOM.nextDouble();
            double u2 = RANDOM.nextDouble();
            double z0 = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
            return mean + stdDev * z0;
        } else {
            double u1 = RANDOM.nextDouble();
            double u2 = RANDOM.nextDouble();
            double z1 = Math.sqrt(-2 * Math.log(u1)) * Math.sin(2 * Math.PI * u2);
            return mean + stdDev * z1;
        }
    }

    /**
     * Генерирует значение по бета-распределению (упрощенная версия)
     * Концентрирует значения в центре диапазона
     */
    private double generateBeta(double alpha, double beta, double min, double max) {
        // Упрощенная реализация бета-распределения
        double u1 = Math.pow(RANDOM.nextDouble(), 1.0 / alpha);
        double u2 = Math.pow(RANDOM.nextDouble(), 1.0 / beta);
        double betaValue = u1 / (u1 + u2);
        return min + betaValue * (max - min);
    }

    /**
     * Генерирует значение по экспоненциальному распределению
     */
    private double generateExponential(double lambda) {
        return -Math.log(1 - RANDOM.nextDouble()) / lambda;
    }

    /**
     * Генерирует значение по пуассонову распределению
     */
    private int generatePoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= RANDOM.nextDouble();
        } while (p > L);

        return k - 1;
    }

    /**
     * Генерирует значение по треугольному распределению
     */
    private double generateTriangular(double min, double max, double mode) {
        double u = RANDOM.nextDouble();
        double fc = (mode - min) / (max - min);

        if (u < fc) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1 - u) * (max - min) * (max - mode));
        }
    }

    public List<DeviceData> randomDevices(int count) {
        List<DeviceData> deviceData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            deviceData.add(randomDevice());
        }
        return deviceData;
    }

    public DeviceData randomDevice() {
        long id = idCounter++;
        Type type = Type.values()[RANDOM.nextInt(Type.values().length)];
        String name = generateRealisticName(type, id);
        String manufacturer = MANUFACTURERS[RANDOM.nextInt(MANUFACTURERS.length)];

        List<String> capabilities = generateRealisticCapabilities(type);
        Location location = generateRealisticLocation();
        Status status = generateRealisticStatus(type);

        return new DeviceData(id, name, manufacturer, type, capabilities, location, status);
    }

    private String generateRealisticName(Type type, long id) {
        String[] names = DEVICE_NAMES_BY_TYPE.get(type);
        String baseName = names[RANDOM.nextInt(names.length)];

        // Добавляем номер комнаты или зоны для реалистичности
        String roomSuffix = generateRoomSuffix();
        return baseName + "_" + roomSuffix + "_" + id;
    }

    private String generateRoomSuffix() {
        String[] rooms = {
                "LivingRoom", "Bedroom", "Kitchen", "Bathroom", "Garage",
                "Basement", "Attic", "Office", "Hallway", "DiningRoom",
                "GuestRoom", "Study", "Laundry", "Pantry", "Closet"
        };
        return rooms[RANDOM.nextInt(rooms.length)];
    }

    private List<String> generateRealisticCapabilities(Type type) {
        List<String> result = new ArrayList<>();
        String[] typeCapabilities = CAPABILITIES_BY_TYPE.get(type);

        // Используем пуассоново распределение для количества возможностей
        // Среднее значение зависит от типа устройства
        double lambda = getCapabilityLambda(type);
        int capabilityCount = Math.max(2, Math.min(generatePoisson(lambda), typeCapabilities.length));

        // Добавляем возможности специфичные для типа
        while (result.size() < capabilityCount && result.size() < typeCapabilities.length) {
            String cap = typeCapabilities[RANDOM.nextInt(typeCapabilities.length)];
            if (!result.contains(cap)) {
                result.add(cap);
            }
        }

        // С небольшой вероятностью добавляем дополнительные общие возможности
        if (RANDOM.nextDouble() < 0.3) {
            String commonCap = COMMON_CAPABILITIES[RANDOM.nextInt(COMMON_CAPABILITIES.length)];
            if (!result.contains(commonCap)) {
                result.add(commonCap);
            }
        }

        return result;
    }

    private double getCapabilityLambda(Type type) {
        // Разные типы устройств имеют разное среднее количество возможностей
        return switch (type) {
            case GATEWAY -> 6.0;
            case CAMERA -> 5.0;
            case ACTUATOR_LIGHT -> 4.5;
            case ACTUATOR_LOCK -> 4.0;
            case SMART_PLUG -> 2.0;
            case SENSOR_TEMPERATURE, SENSOR_HUMIDITY -> 3.0;
            default -> 3.5;
        };
    }

    private Location generateRealisticLocation() {
        // Используем бета-распределение для концентрации устройств в центре дома
        // X, Y - координаты в метрах (0-50м для дома)
        // Z - этаж (0-3 для типичного дома)

        // Бета-распределение концентрирует устройства в центре дома
        double xRaw = generateBeta(2.0, 2.0, 0, 50);
        double yRaw = generateBeta(2.0, 2.0, 0, 50);

        // Этажи распределены по треугольному распределению (больше на первом этаже)
        double zRaw = generateTriangular(0, 3, 1.0);

        int x = Math.max(0, Math.min(49, (int) Math.round(xRaw)));
        int y = Math.max(0, Math.min(49, (int) Math.round(yRaw)));
        int z = Math.max(0, Math.min(3, (int) Math.round(zRaw)));

        return new Location(x, y, z);
    }

    private Status generateRealisticStatus(Type type) {
        // Используем нормальное распределение для определения онлайн статуса
        // Создаем корреляцию между типом устройства и вероятностью быть онлайн
        double onlineProbability = getOnlineProbability(type);
        boolean isOnline = RANDOM.nextDouble() < onlineProbability;

        int batteryLevel;
        int signalStrength;
        LocalDateTime lastHeartbeat;

        if (isOnline) {
            // Для онлайн устройств используем нормальное распределение
            batteryLevel = generateBatteryLevel(type);
            signalStrength = generateSignalStrength();

            // Добавляем корреляцию: слабый сигнал влияет на батарею
            if (signalStrength < 30) {
                batteryLevel = Math.max(0, batteryLevel - 10);
            }

            // Heartbeat распределен экспоненциально (больше недавних)
            int minutesAgo = (int) Math.min(30, generateExponential(0.1));
            lastHeartbeat = LocalDateTime.now().minusMinutes(minutesAgo);
        } else {
            // Для офлайн устройств - коррелированные низкие значения
            batteryLevel = generateOfflineBatteryLevel(type);
            signalStrength = generateOfflineSignalStrength();

            // Сильная корреляция для офлайн устройств
            if (batteryLevel < 20) {
                signalStrength = Math.max(0, signalStrength - 15);
            }

            // Heartbeat давно - экспоненциальное распределение
            int hoursAgo = (int) Math.min(24, generateExponential(0.3));
            lastHeartbeat = LocalDateTime.now().minusHours(Math.max(1, hoursAgo));
        }

        return new Status(isOnline, batteryLevel, signalStrength, lastHeartbeat);
    }

    private double getOnlineProbability(Type type) {
        // Разные типы устройств имеют разную вероятность быть онлайн
        return switch (type) {
            case GATEWAY -> 0.95; // Шлюзы почти всегда онлайн
            case SMART_PLUG -> 0.90; // Розетки обычно онлайн
            case ACTUATOR_LIGHT -> 0.85;
            case ACTUATOR_LOCK -> 0.80;
            case CAMERA -> 0.75; // Камеры могут быть отключены
            case SENSOR_TEMPERATURE, SENSOR_HUMIDITY -> 0.70; // Датчики часто на батарее
            default -> 0.80;
        };
    }

    private int generateBatteryLevel(Type type) {
        BatteryDistribution dist = getBatteryDistribution(type);

        double batteryRaw = generateNormal(dist.mean, dist.stdDev);

        return (int) Math.round(Math.max(0, Math.min(100, batteryRaw)));
    }

    private int generateOfflineBatteryLevel(Type type) {
        BatteryDistribution dist = getBatteryDistribution(type);

        double batteryRaw = generateNormal(dist.mean * 0.3, dist.stdDev * 0.5);

        return (int) Math.round(Math.max(0, Math.min(100, batteryRaw)));
    }

    private BatteryDistribution getBatteryDistribution(Type type) {
        return switch (type) {
            case GATEWAY -> new BatteryDistribution(95, 10); // Высокая батарея, низкое отклонение
            case SMART_PLUG -> new BatteryDistribution(100, 0); // Всегда полная
            case ACTUATOR_LIGHT, ACTUATOR_LOCK -> new BatteryDistribution(85, 15); // Высокая, но с отклонением
            case CAMERA -> new BatteryDistribution(70, 20); // Средняя с большим отклонением
            case SENSOR_TEMPERATURE, SENSOR_HUMIDITY -> new BatteryDistribution(60, 25); // Средняя, много отклонений
            default -> new BatteryDistribution(70, 20);
        };
    }

    private static class BatteryDistribution {
        final double mean;
        final double stdDev;

        BatteryDistribution(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }

    private int generateSignalStrength() {
        // Используем нормальное распределение для силы сигнала
        // Среднее 70%, стандартное отклонение 20%
        double signalRaw = generateNormal(70, 20);

        return (int) Math.round(Math.max(0, Math.min(100, signalRaw)));
    }

    private int generateOfflineSignalStrength() {
        // Для офлайн устройств сигнал обычно слабый
        // Среднее 25%, стандартное отклонение 15%
        double signalRaw = generateNormal(25, 15);

        return (int) Math.round(Math.max(0, Math.min(100, signalRaw)));
    }

    public DeviceData updateDevice(DeviceData deviceData) {
        // Update Status
        Status currentStatus = deviceData.status();
        // Simulate small changes
        boolean isOnline = currentStatus.isOnline();

        // 1% chance to toggle online/offline
        if (RANDOM.nextDouble() < 0.01) {
            isOnline = !isOnline;
        }

        int batteryLevel = currentStatus.batteryLevel();
        // 0.5% chance to decrease battery if online
        if (isOnline && batteryLevel > 0 && RANDOM.nextDouble() < 0.005) {
            batteryLevel--;
        }

        int signalStrength = currentStatus.signalStrength();
        // Fluctuate signal strength +/- 5
        if (isOnline) {
            int change = RANDOM.nextInt(11) - 5;
            signalStrength = Math.max(0, Math.min(100, signalStrength + change));
        }

        // Update heartbeat
        java.time.LocalDateTime lastHeartbeat = isOnline ? java.time.LocalDateTime.now()
                : currentStatus.lastHeartbeat();

        Status newStatus = new Status(isOnline, batteryLevel, signalStrength, lastHeartbeat);
        DeviceData updated = deviceData.withStatus(newStatus);

        // Update Location (Random Walk) - 10% chance to move
        if (RANDOM.nextDouble() < 0.1) {
            Location loc = updated.location();
            int dx = RANDOM.nextInt(3) - 1; // -1, 0, 1
            int dy = RANDOM.nextInt(3) - 1;
            int newX = Math.max(0, Math.min(50, loc.x() + dx));
            int newY = Math.max(0, Math.min(50, loc.y() + dy));
            updated = updated.withLocation(new Location(newX, newY, loc.z()));
        }
        return updated;
    }
}
