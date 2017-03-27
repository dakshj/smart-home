package com.smarthome.model.config;

import com.smarthome.enums.EntrantType;
import com.smarthome.model.Address;

import java.io.Serializable;

public class EntrantConfig extends Config implements Serializable {

    private Address gatewayAddress;

    private EntrantType entrantType;

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public EntrantType getEntrantType() {
        return entrantType;
    }
}
