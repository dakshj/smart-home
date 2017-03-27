package com.smarthome.model.config;

import com.smarthome.enums.SensorType;
import com.smarthome.model.Address;

import java.io.Serializable;

public class SensorConfig extends Config implements Serializable {

    private Address gatewayAddress;

    private SensorType sensorType;

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public SensorType getSensorType() {
        return sensorType;
    }
}
