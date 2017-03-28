package com.smarthome.ioT.sensor;

import com.smarthome.ioT.IoTServer;
import com.smarthome.model.Address;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public interface SensorServer extends IoTServer, Remote {

    String NAME = "Sensor Server";

    /**
     * Establishes a connection with a {@link SensorServer}.
     *
     * @param address The address of the {@link SensorServer}
     * @return An instance of the connected {@link SensorServer}, connected remotely via Java RMI
     * @throws RemoteException   Thrown when a Java RMI exception occurs
     * @throws NotBoundException Thrown when the remote binding does not exist in the {@link Registry}
     */
    static SensorServer connect(final Address address)
            throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNo());
        return (SensorServer) registry.lookup(NAME);
    }

    /**
     * Returns the current state of this sensor.
     *
     * @param senderLogicalTime The logical time of the calling IoT server
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void queryState(final long senderLogicalTime) throws RemoteException;

    void triggerMotionSensor() throws RemoteException, NotBoundException;

    void toggleDoorSensor() throws RemoteException, NotBoundException;

    /**
     * Sets the authorized user who can change the states of all devices.
     *
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setAuthorizedEntrant() throws RemoteException;

    /**
     * Returns boolean value to check if authorized user or intruder is
     * trigerring events.
     *
     * @return {@code isAuthorizedUser} The boolean flag which denotes whether
     * the user is authorized or not
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    boolean isAuthorizedEntrant() throws RemoteException;
}
