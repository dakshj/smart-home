package com.smarthome.device.server;

import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class DeviceServerImpl implements DeviceServer {

    private final Address selfAddress;
    private final Address gatewayAddress;

    private Device device;

    public DeviceServerImpl(final Device device, final Address selfAddress,
            final Address gatewayAddress) {
        this.device = device;
        this.selfAddress = selfAddress;
        this.gatewayAddress = gatewayAddress;

        try {
            setDevice((Device) GatewayServer.connect(gatewayAddress).register(device, selfAddress));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void queryState() throws RemoteException {
        try {
            GatewayServer.connect(gatewayAddress).reportState(getDevice());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private Device getDevice() {
        return device;
    }

    private void setDevice(final Device device) {
        this.device = device;
    }
}
