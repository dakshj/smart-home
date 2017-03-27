package com.smarthome.ioT.gateway;

import com.smarthome.ioT.db.DbServer;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.enums.IoTType;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.config.GatewayConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;
import com.smarthome.ioT.sensor.SensorServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class GatewayServerImpl extends UnicastRemoteObject implements GatewayServer {

    private final Map<IoT, Address> registeredIoTs;
    private final IoT ioT;
    private final GatewayConfig gatewayConfig;

    private long synchronizationOffset;
    private long logicalTime;

    public GatewayServerImpl(final GatewayConfig gatewayConfig) throws RemoteException {
        this.gatewayConfig = gatewayConfig;

        ioT = new IoT(UUID.randomUUID(), IoTType.GATEWAY);

        registeredIoTs = new HashMap<>();

        registeredIoTs.put(ioT, getGatewayConfig().getAddress());

        startServer(gatewayConfig.getAddress().getPortNo());

        waitForUserToStartLeaderElectionAndTimeSync();
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
    public IoT getIoT() {
        return ioT;
    }

    @Override
    public Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) throws RemoteException {
        // No-op
    }

    @Override
    public long getSynchronizationOffset() {
        return synchronizationOffset;
    }

    @Override
    public void setSynchronizationOffset(final long synchronizationOffset) throws RemoteException {
        this.synchronizationOffset = synchronizationOffset;
    }

    @Override
    public void register(final IoT ioT, final Address address) throws RemoteException {
        final UUID uuid = UUID.randomUUID();

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
                break;

            case DEVICE:
                Device device = (Device) ioT;

                device = new Device(uuid, device.getIoTType(), device.getDeviceType());

                registeredIoTs.put(device, address);
                break;
        }
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
            dbServer = DbServer.connect(getGatewayConfig().getDbAddress());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        assert dbServer != null;

        switch (ioT.getIoTType()) {
            case SENSOR:
                final Sensor sensor = ((Sensor) ioT);

                switch (sensor.getSensorType()) {
                    case TEMPERATURE:
                        final TemperatureSensor temperatureSensor = ((TemperatureSensor) sensor);
                        dbServer.temperatureChanged(temperatureSensor, time);
                        break;

                    case MOTION:
                        final MotionSensor motionSensor = ((MotionSensor) sensor);
                        dbServer.motionDetected(motionSensor, time);
                        break;

                    case DOOR:
                        final DoorSensor doorSensor = ((DoorSensor) sensor);
                        dbServer.doorToggled(doorSensor, time);
                        break;
                }
                break;

            case DEVICE:
                final Device device = ((Device) ioT);
                dbServer.deviceToggled(device, time);
                break;
        }
    }

    @Override
    public void setDeviceState(final Device device, final boolean state) {
        try {
            DeviceServer.connect(registeredIoTs.get(device)).setState(state);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void waitForUserToStartLeaderElectionAndTimeSync() {
        System.out.println("Please press Enter after all IoT servers are running.\n" +
                "Pressing Enter will being the Leader Election and Time Synchronization jobs.");
        new Scanner(System.in).next();
        electLeader();
    }

    /**
     * Elects a leader using the
     * <a href="https://en.wikipedia.org/wiki/Bully_algorithm">Bully algorithm</a>.
     */
    private void electLeader() {
        if (isLeader()) {
            synchronizeTime();
        } else {
            broadcastRegisteredIoTs();
        }
    }

    /**
     * Broadcasts the {@link Map} of all registered IoTs to each IoT.
     */
    private void broadcastRegisteredIoTs() {
        // Send to DB
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.DB)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> {
                    try {
                        DbServer.connect(address).setRegisteredIoTs(getRegisteredIoTs());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Send to all Sensors
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.SENSOR)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address).setRegisteredIoTs(getRegisteredIoTs());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Send to all Devices
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.DEVICE)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> {
                    try {
                        DeviceServer.connect(address).setRegisteredIoTs(getRegisteredIoTs());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }
}
