package com.smarthome.gateway.server;

import com.smarthome.db.server.DbServer;
import com.smarthome.device.server.DeviceServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;
import com.smarthome.sensor.server.SensorServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GatewayServerImpl extends UnicastRemoteObject implements GatewayServer {

    private final Map<IoT, Address> registeredIoTs;
    private final Address dbAddress;

    private long synchronizationOffset;

    public GatewayServerImpl(final Address selfAddress, final Address dbAddress)
            throws RemoteException {
        this.dbAddress = dbAddress;

        registeredIoTs = new HashMap<>();

        startServer(selfAddress.getPortNo());
    }

    /**
     * Starts the Gateway Server on the provided port number.
     * <p>
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the Gateway Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    @Override
    public IoT register(final IoT ioT, final Address address) throws RemoteException {
        final UUID uuid = getRandomUUID();

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

                registeredIoTs.put(sensor, address);

                return sensor;

            case DEVICE:
                Device device = (Device) ioT;

                device = new Device(uuid, device.getIoTType(), device.getDeviceType());

                registeredIoTs.put(device, address);

                return device;
        }

        return null;
    }

    @Override
    public void queryState(final IoT ioT) {
        if (registeredIoTs.containsKey(ioT)) {
            try {
                switch (ioT.getIoTType()) {
                    case SENSOR:
                        SensorServer.connect(registeredIoTs.get(ioT)).queryState();
                        break;

                    case DEVICE:
                        DeviceServer.connect(registeredIoTs.get(ioT)).queryState();
                        break;
                }
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reportState(final IoT ioT, final long time) throws RemoteException {
        DbServer dbServer = null;

        try {
            dbServer = DbServer.connect(getDbAddress());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        switch (ioT.getIoTType()) {
            case SENSOR:
                final Sensor sensor = ((Sensor) ioT);

                switch (sensor.getSensorType()) {
                    case TEMPERATURE:
                        final TemperatureSensor temperatureSensor = ((TemperatureSensor) sensor);
                        assert dbServer != null;
                        dbServer.temperatureChanged(temperatureSensor, time);
                        break;

                    case MOTION:
                        final MotionSensor motionSensor = ((MotionSensor) sensor);
                        assert dbServer != null;
                        dbServer.motionDetected(motionSensor, time);
                        break;

                    case DOOR:
                        final DoorSensor doorSensor = ((DoorSensor) sensor);
                        assert dbServer != null;
                        dbServer.doorToggled(doorSensor, time);
                        break;
                }
                break;

            case DEVICE:
                final Device device = ((Device) ioT);
                assert dbServer != null;
                dbServer.deviceToggled(device, time);

                break;
        }
    }

    @Override
    public void changeDeviceState(final Device device, final boolean state) {
        try {
            DeviceServer.connect(registeredIoTs.get(device)).changeState(state);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<IoT> getIoTs() {
        return registeredIoTs.keySet();
    }

    /**
     * Generates a random UUID which is not present in {@code registeredIoTs}.
     *
     * @return The randomly generated UUID
     */
    private UUID getRandomUUID() {
        final UUID uuid = UUID.randomUUID();

        if (registeredIoTs.keySet().stream()
                .map(IoT::getId)
                .filter(streamId -> streamId.equals(uuid))
                .count() > 0) {
            return getRandomUUID();
        }

        return uuid;
    }

    private Address getDbAddress() {
        return dbAddress;
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
