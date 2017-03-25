package com.smarthome.sensor.server;

import com.smarthome.model.Address;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public interface SensorServer extends Remote {

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
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void queryState() throws RemoteException;
}
