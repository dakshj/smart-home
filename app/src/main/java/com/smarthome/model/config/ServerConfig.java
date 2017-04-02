package com.smarthome.model.config;

import com.smarthome.model.Address;

import java.io.Serializable;

public class ServerConfig extends Config implements Serializable {

    private Address address;
    private long randomTimeOffset;

    public Address getAddress() {
        return address;
    }

    public long getRandomTimeOffset() {
        return randomTimeOffset;
    }
}
