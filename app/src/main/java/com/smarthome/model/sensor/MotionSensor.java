package com.smarthome.model.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;

import java.io.Serializable;
import java.util.UUID;

public class MotionSensor extends Sensor<Void> implements Serializable {

    public MotionSensor(final UUID id, final IoTType ioTType, final SensorType sensorType) {
        super(id, ioTType, sensorType);
    }
}
