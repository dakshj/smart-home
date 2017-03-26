package com.smarthome.model.config;

import com.smarthome.model.Address;

public class GatewayConfig {

    private Address address;

    private Address dbAddress;

    public Address getAddress() {
        return address;
    }

    public Address getDbAddress() {
        return dbAddress;
    }
}
