package com.smarthome;

import com.smarthome.enums.ExecutionMode;

import java.rmi.RemoteException;

public class Main {
    /**
     * @param args <p>
     *             args[0]:
     *             <p>
     *             Use "0" to start Gateway Server
     *             <p>
     *             Use "1" to start DB Server
     *             <p>
     *             Use "2" to start Sensor Server
     *             <p>
     *             Use "3" to start Device Server
     *             <p>
     *             Use "4" to start User Server
     *             <p>
     *             args[1]:
     *             <i>For Sensor:</i> The type of sensor, i.e. Temperature, Motion, Door
     *             <p>
     *             <i>For Device:</i> The type of smart device, i.e. Bulb, Outlet
     *             <p>
     *             <i>For Entrant:</i> The type of entrant, i.e. User, Intruder
     * </p>
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    public static void main(String[] args) throws RemoteException {

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No command-line arguments provided." +
                    " Please refer the JavaDoc to know more on these arguments.");
        }

        ExecutionMode executionMode;

        try {
            executionMode = ExecutionMode.from(Integer.parseInt(args[0]));
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Execution Mode is invalid.");
        }

        assert executionMode != null;

        switch (executionMode) {
            case GATEWAY:
                break;

            case DB:
                break;

            case SENSOR:
                break;

            case DEVICE:
                break;

            case ENTRANT:
                break;
        }
    }

}
