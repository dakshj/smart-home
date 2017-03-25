package com.smarthome.gateway.server;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.SensorType;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.Sensor;

import java.rmi.RemoteException;

public class GatewayServerImpl implements GatewayServer {

    @Override
    public Sensor register(final SensorType sensorType, final Address address) throws RemoteException {
        return null;
    }

    @Override
    public Device register(final DeviceType deviceType, final Address address) throws RemoteException {
        return null;
    }

    @Override
    public void queryState(final long id) {

    }

    @Override
    public void reportState(final Sensor sensor) throws RemoteException {

    }

    @Override
    public void reportState(final Device device) throws RemoteException {

    }

    @Override
    public void changeDeviceState(final long id, final boolean state) {

    }
}
