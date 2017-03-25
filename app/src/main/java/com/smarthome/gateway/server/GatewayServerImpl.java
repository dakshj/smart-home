package com.smarthome.gateway.server;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.SensorType;
import com.smarthome.model.Address;
import com.smarthome.model.Bulb;
import com.smarthome.model.Device;
import com.smarthome.model.DoorSensor;
import com.smarthome.model.MotionSensor;
import com.smarthome.model.Outlet;
import com.smarthome.model.Sensor;
import com.smarthome.model.TemperatureSensor;

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
    public Sensor register(final SensorType sensorType, final Address address) throws RemoteException {
        final UUID uuid = getRandomUUID();
        registeredIoTs.put(uuid, address);
        Sensor sensor = null;

        switch (sensorType) {
            case TEMPERATURE:
                sensor = new TemperatureSensor(uuid);
                break;

            case MOTION:
                sensor = new MotionSensor(uuid);
                break;

            case DOOR:
                sensor = new DoorSensor(uuid);
                break;
        }

        return sensor;
    }

    @Override
    public Device register(final DeviceType deviceType, final Address address) throws RemoteException {
        final UUID uuid = getRandomUUID();
        registeredIoTs.put(uuid, address);
        Device device = null;

        switch (deviceType) {
            case BULB:
                device = new Bulb(uuid);
                break;

            case OUTLET:
                device = new Outlet(uuid);
                break;
        }

        return device;
    }

    @Override
    public void queryState(final UUID id) {

    }

    @Override
    public void reportState(final Sensor sensor) throws RemoteException {

    }

    @Override
    public void reportState(final Device device) throws RemoteException {

    }

    @Override
    public void changeDeviceState(final UUID id, final boolean state) {

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
