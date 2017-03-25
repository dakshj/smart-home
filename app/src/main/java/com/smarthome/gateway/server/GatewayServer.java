package com.smarthome.gateway.server;

import com.smarthome.model.Address;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public interface GatewayServer extends Remote {

    String NAME = "Gateway Server";

    /**
     * Establishes a connection with a {@link GatewayServer}.
     *
     * @param address The address of the {@link GatewayServer}
     * @return An instance of the connected {@link GatewayServer}, connected remotely via Java RMI
     * @throws RemoteException   Thrown when a Java RMI exception occurs
     * @throws NotBoundException Thrown when the remote binding does not exist in the {@link Registry}
     */
    static GatewayServer connect(final Address address)
            throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNo());
        return (GatewayServer) registry.lookup(NAME);
    }
}
