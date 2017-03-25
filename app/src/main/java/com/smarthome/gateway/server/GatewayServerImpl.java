package com.smarthome.gateway.server;

import com.smarthome.model.Address;
import com.smarthome.model.device.Bulb;
import com.smarthome.model.device.Device;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.IoT;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.device.Outlet;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;
import com.smarthome.sensor.server.SensorServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GatewayServerImpl implements GatewayServer {

    private final Map<UUID, Address> registeredIoTs;

    public GatewayServerImpl() {
        registeredIoTs = new HashMap<>();
    }

    @Override
    public IoT register(final IoT ioT, final Address address) throws RemoteException {
        final UUID uuid = getRandomUUID();
        registeredIoTs.put(uuid, address);

        switch (ioT.getIoTType()) {
            case SENSOR:
                Sensor sensor = ((Sensor) ioT);

                switch (sensor.getSensorType()) {
                    case TEMPERATURE:
                        sensor = new TemperatureSensor(uuid, sensor.getIoTType(), sensor.getSensorType());
                        break;

                    case MOTION:
                        sensor = new MotionSensor(uuid, sensor.getIoTType(), sensor.getSensorType());
                        break;

                    case DOOR:
                        sensor = new DoorSensor(uuid, sensor.getIoTType(), sensor.getSensorType());
                        break;
                }

                return sensor;

            case DEVICE:
                Device device = (Device) ioT;

                switch (device.getDeviceType()) {
                    case BULB:
                        device = new Bulb(uuid, device.getIoTType(), device.getDeviceType());
                        break;

                    case OUTLET:
                        device = new Outlet(uuid, device.getIoTType(), device.getDeviceType());
                        break;
                }

                return device;
        }

        return null;
    }

    @Override
    public void queryState(final IoT ioT) {
        if (registeredIoTs.containsKey(ioT.getId())) {
            try {
                SensorServer.connect(registeredIoTs.get(ioT.getId())).queryState();
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reportState(final IoT ioT) throws RemoteException {

    }

    @Override
    public void changeDeviceState(final Device device, final boolean state) {

    }

    /**
     * Generates a random UUID which is not present in {@code registeredIoTs}.
     *
     * @return The randomly generated UUID
     */
    private UUID getRandomUUID() {
        UUID uuid = UUID.randomUUID();
        if (registeredIoTs.containsKey(uuid)) {
            uuid = getRandomUUID();
        }
        return uuid;
    }
}
