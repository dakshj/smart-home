package com.smarthome;

import com.smarthome.db.server.DbServerImpl;
import com.smarthome.device.server.DeviceServerImpl;
import com.smarthome.entrant.server.EntrantServerImpl;
import com.smarthome.enums.ExecutionMode;
import com.smarthome.gateway.server.GatewayServerImpl;
import com.smarthome.model.Address;
import com.smarthome.sensor.server.SensorServerImpl;

import java.rmi.RemoteException;

public class Main {
    /**
     * @param args <p>
     *             args[0]:
     *             <p>
     *             This server's port number
     *             <p>
     *             <p>
     *             args[1]:
     *             <p>
     *             Use "0" to start a Gateway Server
     *             <p>
     *             Use "1" to start a DB Server
     *             <p>
     *             Use "2" to start a Sensor Server
     *             <p>
     *             Use "3" to start a Device Server
     *             <p>
     *             Use "4" to start an Entrant Server
     *             <p>
     *             <p>
     *             args[2]:
     *             <p>
     *             <i>For Sensor:</i>
     *             <p>
     *             Use "0" for a Temperature Sensor
     *             <p>
     *             Use "1" for a Motion Sensor
     *             <p>
     *             Use "2" for a Door Sensor
     *             <p>
     *             <i>For Device:</i>
     *             <p>
     *             Use "0" for a Bulb
     *             <p>
     *             Use "1" for an Outlet
     *             <p>
     *             <i>For Entrant:</i>
     *             <p>
     *             Use "0" for a User
     *             <p>
     *             Use "1" for an Intruder
     *             <p>
     *             <i>For Gateway Server:</i>
     *             <p>
     *             DB Server's host
     *             <p>
     *             <p>
     *             args[3]:
     *             <p>
     *             <i>For Gateway Server:</i>
     *             <p>
     *             DB Server's port number
     *             <p>
     *             <i>For Sensors, Devices and Entrants:</i>
     *             <p>
     *             Gateway Server's host
     *             <p>
     *             <p>
     *             args[4]:
     *             <p>
     *             <i>For Sensors, Devices and Entrants only</i>:
     *             <p>
     *             Gateway Server's port number
     *             <p>
     *             <p>
     *             args[5]:
     *             <p>
     *             <i>For Entrants only</i>:
     *             <p>
     *             {@code true} if the Entrant has an authorization beacon attached to her keychain;
     *             {@code false} otherwise
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    public static void main(String[] args) throws RemoteException {

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No command-line arguments provided." +
                    " Please refer the JavaDoc to know more on these arguments.");
        }

        int portNo;

        try {
            portNo = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port Number is invalid");
        }

        ExecutionMode executionMode;

        try {
            executionMode = ExecutionMode.from(Integer.parseInt(args[1]));
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Execution Mode is invalid.");
        }

        assert executionMode != null;

        final Address gatewayAddress = null;
        final Address dbAddress = null;

        switch (executionMode) {
            case GATEWAY:
                new GatewayServerImpl(gatewayAddress, dbAddress);
                break;

            case DB:
                new DbServerImpl();
                break;

            case SENSOR:
                // TODO Pass sensor, selfAddress and gatewayAddress
                new SensorServerImpl(null, null, gatewayAddress);
                break;

            case DEVICE:
                // TODO Pass device, selfAddress and gatewayAddress
                new DeviceServerImpl(null, null, gatewayAddress);
                break;

            case ENTRANT:
                new EntrantServerImpl();
                break;
        }
    }
}
