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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final IoT ioT = (IoT) o;

        return id != null ? id.equals(ioT.id) : ioT.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
