package com.smarthome.model;

import java.util.UUID;

public class Device {

    private final UUID id;
    private boolean status;

    Device(final UUID id) {
        this.id = id;
    }

    private UUID getId() {
        return id;
    }

    private boolean isStatus() {
        return status;
    }

    private void setStatus(final boolean status) {
        this.status = status;
    }
}
