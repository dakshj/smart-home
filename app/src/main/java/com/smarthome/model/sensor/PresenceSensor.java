package com.smarthome.model.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;

import java.util.UUID;

public class PresenceSensor extends Sensor<Void> {

    private boolean authorizedEntrant;

    public PresenceSensor(final UUID id, final IoTType ioTType, final SensorType sensorType) {
        super(id, ioTType, sensorType);
        authorizedEntrant = false;
    }

    public void setAuthorizedEntrant() {
        authorizedEntrant = true;
    }

    public boolean isAuthorizedEntrant() {
        return authorizedEntrant;
    }
}
