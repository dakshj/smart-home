package com.smarthome.model.config;

import com.smarthome.enums.EntrantType;
import com.smarthome.model.Address;

import java.io.Serializable;

public class EntrantConfig extends Config implements Serializable {

    private Address gatewayAddress;

    private EntrantType entrantType;

    private boolean authorized;

    public Address getGatewayAddress() {
        return gatewayAddress;
    }

    public EntrantType getEntrantType() {
        return entrantType;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
