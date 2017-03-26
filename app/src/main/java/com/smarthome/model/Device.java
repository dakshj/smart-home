package com.smarthome.model;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.IoTType;

import java.util.UUID;

public class Device extends IoT {

    private final DeviceType deviceType;

    private boolean state;

    public Device(final UUID id, final IoTType ioTType, final DeviceType deviceType) {
        super(id, ioTType);
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    private boolean getState() {
        return state;
    }

    public void setState(final boolean state) {
        this.state = state;
    }
}
