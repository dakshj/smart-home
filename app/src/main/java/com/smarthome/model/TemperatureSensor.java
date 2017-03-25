package com.smarthome.model;

import java.io.Serializable;

public class TemperatureSensor extends Sensor<Double> implements Serializable {

    private Double oldData;

    private TemperatureSensor(final long id) {
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
