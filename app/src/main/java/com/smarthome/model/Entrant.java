package com.smarthome.model;

import com.smarthome.enums.EntrantType;

public class Entrant {

    private final EntrantType entrantType;

    public Entrant(final EntrantType entrantType) {
        this.entrantType = entrantType;
    }

    private EntrantType getEntrantType() {
        return entrantType;
    }
}
