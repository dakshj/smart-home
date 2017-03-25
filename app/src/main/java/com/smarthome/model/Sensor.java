package com.smarthome.model;

import java.io.Serializable;
import java.util.UUID;

public class Sensor<T> implements Serializable {

    private final UUID id;
    private T data;

    Sensor(final UUID id) {
        this.id = id;
    }

    private UUID getId() {
        return id;
    }

    T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }
}
