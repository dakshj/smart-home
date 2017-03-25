package com.smarthome.model.device;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.IoTType;

import java.util.UUID;

public class Bulb extends Device {

    public Bulb(final UUID id, final IoTType ioTType, final DeviceType deviceType) {
        super(id, ioTType, deviceType);
    }
}
