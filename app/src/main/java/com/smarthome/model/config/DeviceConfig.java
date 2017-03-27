package com.smarthome.model.config;

import com.smarthome.enums.DeviceType;
import com.smarthome.model.Address;

import java.io.Serializable;

public class DeviceConfig extends Config implements Serializable {

    private Address gatewayAddress;

    private DeviceType deviceType;

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }
}
