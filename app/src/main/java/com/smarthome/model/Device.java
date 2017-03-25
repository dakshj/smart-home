package com.smarthome.model;

class Device {

    private final long id;
    private boolean status;

    Device(final long id) {
        this.id = id;
    }

    private long getId() {
        return id;
    }

    private boolean isStatus() {
        return status;
    }

    private void setStatus(final boolean status) {
        this.status = status;
    }
}
