package com.smarthome.enums;

public enum EntrantType {

    /**
     * Indicates that the Entrant is an authorized User.
     */
    USER,

    /**
     * Indicates that the Entrant is an unauthorized entrant, say, a burglar.
     */
    INTRUDER;

    public static EntrantType from(final int mode) {
        switch (mode) {
            case 0:
                return USER;

            case 1:
                return INTRUDER;
        }

        return null;
    }
}
