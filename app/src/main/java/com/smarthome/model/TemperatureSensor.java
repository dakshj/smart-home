package com.smarthome.model;

import java.io.Serializable;
import java.util.UUID;

public class TemperatureSensor extends Sensor<Double> implements Serializable {

    private Double oldData;

    public TemperatureSensor(final UUID id) {
        super(id);
    }

    @Override
    public void setData(final Double data) {
        setOldData(getData());
        super.setData(data);
    }

    private Double getOldData() {
        return oldData;
    }

    private void setOldData(final Double oldData) {
        this.oldData = oldData;
    }
}
