package com.smarthome.model.config;

import com.smarthome.model.Address;

import java.io.Serializable;

public class GatewayConfig extends Config implements Serializable {

    private Address dbAddress;

    public Address getDbAddress() {
        return dbAddress;
    }
}
