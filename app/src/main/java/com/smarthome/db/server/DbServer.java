package com.smarthome.db.server;

import com.smarthome.model.Address;
import com.smarthome.model.DoorSensor;
import com.smarthome.model.MotionSensor;
import com.smarthome.model.TemperatureSensor;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public interface DbServer extends Remote {

    String NAME = "DB Server";

    /**
     * Establishes a connection with a {@link DbServer}.
     *
     * @param address The address of the {@link DbServer}
     * @return An instance of the connected {@link DbServer}, connected remotely via Java RMI
     * @throws RemoteException   Thrown when a Java RMI exception occurs
     * @throws NotBoundException Thrown when the remote binding does not exist in the {@link Registry}
     */
    static DbServer connect(final Address address)
            throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNo());
        return (DbServer) registry.lookup(NAME);
    }

    /**
     * Logs a temperature change, as provided by the temperature sensor.
     *
     * @param temperatureSensor The temperature sensor which reported the temperature change
     * @param time              The timestamp of the temperature change
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void temperatureChanged(final TemperatureSensor temperatureSensor, final long time)
            throws RemoteException;

    /**
     * Logs a detected motion, as provided by the motion sensor.
     *
     * @param motionSensor The motion sensor which detected any motion
     * @param time         The timestamp of when the motion was detected
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void motionDetected(final MotionSensor motionSensor, final long time) throws RemoteException;

    /**
     * Logs the opened state of a door, as provided by the door sensor.
     *
     * @param doorSensor The door sensor which reported a door that was opened or closed
     * @param time       The timestamp of when the door was opened or closed
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void doorToggled(final DoorSensor doorSensor, final long time) throws RemoteException;
}
