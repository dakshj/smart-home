package com.smarthome.model.config;

import com.smarthome.enums.SensorType;

import java.io.Serializable;

public class SensorConfig extends Config implements Serializable {

    private SensorType sensorType;

    public SensorType getSensorType() {
        return sensorType;
    }
}
