package com.smarthome.model.config;

import com.smarthome.enums.SensorType;
import com.smarthome.model.Address;

public class SensorConfig {

    private Address address;

    private Address gatewayAddress;

    private SensorType sensorType;

    public Address getAddress() {
        return address;
    }

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public SensorType getSensorType() {
        return sensorType;
    }
}
