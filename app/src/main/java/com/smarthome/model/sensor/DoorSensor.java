package com.smarthome.model.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;

import java.util.UUID;

public class DoorSensor extends Sensor<Boolean> {

    public DoorSensor(final UUID id, final IoTType ioTType, final SensorType sensorType) {
        super(id, ioTType, sensorType);
    }

    /**
     * Returns whether the door is opened or closed.
     *
     * @return {@code true} if the door is opened;
     * {@code false} otherwise
     */
    @Override
    public Boolean getData() {
        return super.getData();
    }
}
