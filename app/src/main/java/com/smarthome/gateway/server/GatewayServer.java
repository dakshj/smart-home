package com.smarthome.gateway.server;

import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;

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

    /**
     * Registers a sensor with the gateway.
     * <p>
     * Stores the UUID and Address of the sensor in a {@link java.util.Map}.
     *
     * @param ioT     The IoT which needs to be registered
     * @param address The address of the IoT Server
     * @return The IoT model object, which needs to be stored at an IoT Server
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    IoT register(final IoT ioT, final Address address) throws RemoteException;

    /**
     * Queries the current state of the IoT.
     *
     * @param ioT The identifier of that ioT
     */
    void queryState(final IoT ioT);

    /**
     * Reports the current state of the sensor.
     *
     * @param ioT The IoT model object, containing the current state of the IoT
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    void reportState(final IoT ioT) throws RemoteException;

    /**
     * Changes the state of the device.
     *
     * @param device The Device whose state needs to be changed
     * @param state  The new state of the device
     */
    void changeDeviceState(final Device device, final boolean state);
}
