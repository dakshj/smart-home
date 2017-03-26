package com.smarthome.device.server;

import com.smarthome.enums.IoTType;
import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Device;
import com.smarthome.model.config.DeviceConfig;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class DeviceServerImpl extends UnicastRemoteObject implements DeviceServer {

    private final DeviceConfig deviceConfig;
    private final Device device;

    private long synchronizationOffset;

    public DeviceServerImpl(final DeviceConfig deviceConfig) throws RemoteException {
        this.deviceConfig = deviceConfig;
        device = new Device(UUID.randomUUID(), IoTType.DEVICE, deviceConfig.getDeviceType());

        startServer(deviceConfig.getAddress().getPortNo());

        try {
            GatewayServer.connect(deviceConfig.getGatewayAddress())
                    .register(device, deviceConfig.getAddress());
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
            GatewayServer.connect(getDeviceConfig().getGatewayAddress())
                    .reportState(getDevice(), getSynchronizedTime());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setState(final boolean state) throws RemoteException {
        getDevice().setState(state);
        queryState();
    }

    @Override
    public void toggleState() throws RemoteException {
        getDevice().setState(!getDevice().getState());
        queryState();
    }

    private DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }

    private Device getDevice() {
        return device;
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
