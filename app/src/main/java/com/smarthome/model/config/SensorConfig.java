package com.smarthome.model.config;

import com.smarthome.enums.SensorType;

import java.io.Serializable;

public class SensorConfig extends ServerConfig implements Serializable {

    private SensorType sensorType;

    public SensorType getSensorType() {
        return sensorType;
    }
}
