package com.smarthome.model.config;

import com.smarthome.model.Address;

import java.io.Serializable;

public class Config implements Serializable {

    private Address address;
    private Address gatewayAddress;
    private long randomTimeOffset;

    public Address getAddress() {
        return address;
    }

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public long getRandomTimeOffset() {
        return randomTimeOffset;
    }
}
