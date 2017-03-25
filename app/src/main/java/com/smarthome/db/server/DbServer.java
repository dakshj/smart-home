package com.smarthome.db.server;

import com.smarthome.model.Address;

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
}
