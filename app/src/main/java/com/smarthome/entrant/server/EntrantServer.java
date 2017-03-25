package com.smarthome.entrant.server;

import com.smarthome.model.Address;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public interface EntrantServer extends Remote {

    String NAME = "Entrant Server";

    /**
     * Establishes a connection with a {@link EntrantServer}.
     *
     * @param address The address of the {@link EntrantServer}
     * @return An instance of the connected {@link EntrantServer}, connected remotely via Java RMI
     * @throws RemoteException   Thrown when a Java RMI exception occurs
     * @throws NotBoundException Thrown when the remote binding does not exist in the {@link Registry}
     */
    static EntrantServer connect(final Address address)
            throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNo());
        return (EntrantServer) registry.lookup(NAME);
    }
}
