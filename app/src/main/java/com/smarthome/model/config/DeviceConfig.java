package com.smarthome.model.config;

import com.smarthome.enums.DeviceType;
import com.smarthome.model.Address;

public class DeviceConfig {

    private Address address;

    private Address gatewayAddress;

    private DeviceType deviceType;

    public Address getAddress() {
        return address;
    }

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }
}
