package com.smarthome.device.server;

import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class DeviceServerImpl extends UnicastRemoteObject implements DeviceServer {

    private final Address gatewayAddress;

    private Device device;
    private long synchronizationOffset;

    public DeviceServerImpl(final Device device, final Address selfAddress,
            final Address gatewayAddress) throws RemoteException {
        this.device = device;
        this.gatewayAddress = gatewayAddress;

        startServer(selfAddress.getPortNo());

        try {
            setDevice((Device) GatewayServer.connect(gatewayAddress).register(device, selfAddress));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the Device Server on the provided port number.
     * <p>
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the Device Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    @Override
    public void queryState() throws RemoteException {
        try {
            GatewayServer.connect(gatewayAddress).reportState(getDevice(), getSynchronizedTime());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void changeState(final boolean state) {
        getDevice().setState(state);
    }

    private Device getDevice() {
        return device;
    }

    private void setDevice(final Device device) {
        this.device = device;
    }

    /**
     * Returns the System's current time after adjustment by adding an offset,
     * calculated using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>
     * for clock synchronization.
     *
     * @return The offset-adjusted System time
     */
    private long getSynchronizedTime() {
        return System.currentTimeMillis() + getSynchronizationOffset();
    }

    private long getSynchronizationOffset() {
        return synchronizationOffset;
    }

    private void setSynchronizationOffset(final long synchronizationOffset) {
        this.synchronizationOffset = synchronizationOffset;
    }
}
