package com.smarthome.model.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;

import java.util.UUID;

public class DoorSensor extends Sensor<Boolean> {

    public DoorSensor(final UUID id, final IoTType ioTType, final SensorType sensorType) {
        super(id, ioTType, sensorType);
    }
}
