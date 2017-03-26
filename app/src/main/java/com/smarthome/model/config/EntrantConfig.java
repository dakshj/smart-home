package com.smarthome.model.config;

import com.smarthome.enums.EntrantType;
import com.smarthome.model.Address;

public class EntrantConfig {

    private Address address;

    private Address gatewayAddress;

    private EntrantType entrantType;

    private Address getAddress() {
        return address;
    }

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public EntrantType getEntrantType() {
        return entrantType;
    }
}
