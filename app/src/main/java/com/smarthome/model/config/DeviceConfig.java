package com.smarthome.model.config;

import com.smarthome.enums.DeviceType;

import java.io.Serializable;

public class DeviceConfig extends Config implements Serializable {

    private DeviceType deviceType;

    public DeviceType getDeviceType() {
        return deviceType;
    }
}
