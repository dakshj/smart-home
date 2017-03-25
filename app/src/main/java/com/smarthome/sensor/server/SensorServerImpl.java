package com.smarthome.sensor.server;

import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.sensor.Sensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class SensorServerImpl implements SensorServer {

    private final Address selfAddress;
    private final Address gatewayAddress;

    private Sensor sensor;

    public SensorServerImpl(final Sensor sensor, final Address selfAddress,
            final Address gatewayAddress) {
        this.sensor = sensor;
        this.selfAddress = selfAddress;
        this.gatewayAddress = gatewayAddress;

        try {
            setSensor((Sensor) GatewayServer.connect(gatewayAddress).register(sensor, selfAddress));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void queryState() throws RemoteException {
        try {
            GatewayServer.connect(gatewayAddress).reportState(getSensor());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private Sensor getSensor() {
        return sensor;
    }

    private void setSensor(final Sensor sensor) {
        this.sensor = sensor;
    }
}
