package com.smarthome.model;

import com.smarthome.enums.EntrantType;

import java.io.Serializable;

public class Entrant implements Serializable {

    private final EntrantType entrantType;

    public Entrant(final EntrantType entrantType) {
        this.entrantType = entrantType;
    }

    private EntrantType getEntrantType() {
        return entrantType;
    }
}
