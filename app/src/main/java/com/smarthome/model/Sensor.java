package com.smarthome.model;

import java.io.Serializable;

class Sensor<T> implements Serializable {

    private final long id;
    private T data;

    Sensor(final long id) {
        this.id = id;
    }

    private long getId() {
        return id;
    }

    T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }
}
