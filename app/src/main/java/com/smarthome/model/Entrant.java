package com.smarthome.model;

public class Entrant {

    private final boolean authorized;

    public Entrant(final boolean authorized) {
        this.authorized = authorized;
    }

    private boolean isAuthorized() {
        return authorized;
    }
}
