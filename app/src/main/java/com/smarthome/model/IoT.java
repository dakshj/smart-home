package com.smarthome.model;

import com.smarthome.enums.IoTType;

import java.util.UUID;

public class IoT {

    private final UUID id;
    private final IoTType ioTType;

    protected IoT(final UUID id, final IoTType ioTType) {
        this.id = id;
        this.ioTType = ioTType;
    }

    public UUID getId() {
        return id;
    }

    public IoTType getIoTType() {
        return ioTType;
    }
}
