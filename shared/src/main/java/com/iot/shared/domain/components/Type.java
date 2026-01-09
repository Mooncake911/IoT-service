package com.iot.shared.domain.components;

public enum Type {
    SENSOR_TEMPERATURE, // Датчик температуры
    SENSOR_HUMIDITY, // Датчик влажности
    ACTUATOR_LIGHT, // Исполнительное устройство (свет)
    ACTUATOR_LOCK, // Исполнительное устройство (замок)
    CAMERA, // Камера
    SMART_PLUG, // Умная розетка
    GATEWAY // Шлюз
}
