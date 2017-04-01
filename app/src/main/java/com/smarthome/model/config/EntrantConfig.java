package com.smarthome.model.config;

import com.smarthome.enums.EntrantType;

import java.io.Serializable;

public class EntrantConfig extends Config implements Serializable {

    private EntrantType entrantType;
    private boolean authorized;

    public EntrantType getEntrantType() {
        return entrantType;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
