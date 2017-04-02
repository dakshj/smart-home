package com.smarthome.model.config;

import com.smarthome.model.Address;

import java.io.Serializable;

public class Config implements Serializable {

    private Address gatewayAddress;

    public Address getGatewayAddress() {
        return gatewayAddress;
    }
}
