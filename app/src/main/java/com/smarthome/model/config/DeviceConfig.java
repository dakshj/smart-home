package com.smarthome.model.config;

import com.smarthome.enums.DeviceType;

import java.io.Serializable;

public class DeviceConfig extends ServerConfig implements Serializable {

    private DeviceType deviceType;

    public DeviceType getDeviceType() {
        return deviceType;
    }
}
