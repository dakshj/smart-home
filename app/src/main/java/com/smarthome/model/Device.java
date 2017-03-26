package com.smarthome.model;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.IoTType;

import java.util.UUID;

public class Device extends IoT {

    private final DeviceType deviceType;

    private boolean status;

    public Device(final UUID id, final IoTType ioTType, final DeviceType deviceType) {
        super(id, ioTType);
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    private boolean isStatus() {
        return status;
    }

    private void setStatus(final boolean status) {
        this.status = status;
    }
}
