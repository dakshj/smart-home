package com.smarthome.ioT.gateway;

import com.smarthome.enums.IoTType;
import com.smarthome.ioT.IoTServerImpl;
import com.smarthome.ioT.db.DbServer;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.ioT.sensor.SensorServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.config.GatewayConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class GatewayServerImpl extends IoTServerImpl implements GatewayServer {

    public GatewayServerImpl(final GatewayConfig gatewayConfig) throws RemoteException {
        super(gatewayConfig, false);

        waitForUserToStartLeaderElectionAndTimeSync();
    }

    @Override
    public IoT createIoT() {
        return new IoT(UUID.randomUUID(), IoTType.GATEWAY);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs,
            final long senderLogicalTime) throws RemoteException {
        // No-op
    }

    @Override
    public void register(final IoT ioT, final Address address, final long senderLogicalTime)
            throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

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

                getRegisteredIoTs().put(sensor, address);
                break;

            case DEVICE:
                Device device = (Device) ioT;

                device = new Device(uuid, device.getIoTType(), device.getDeviceType());

                getRegisteredIoTs().put(device, address);
                break;
        }
    }

    @Override
    public void queryState(final IoT ioT) {
        incrementLogicalTime(0);

        if (getRegisteredIoTs().containsKey(ioT)) {
            try {
                switch (ioT.getIoTType()) {
                    case SENSOR:
                        SensorServer.connect(getRegisteredIoTs().get(ioT)).queryState(getLogicalTime());
                        break;

                    case DEVICE:
                        DeviceServer.connect(getRegisteredIoTs().get(ioT)).queryState(getLogicalTime());
                        break;
                }
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reportState(final IoT ioT, final long time, final long senderLogicalTime)
            throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

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
                        dbServer.temperatureChanged(temperatureSensor, time, senderLogicalTime);
                        break;

                    case MOTION:
                        final MotionSensor motionSensor = ((MotionSensor) sensor);
                        dbServer.motionDetected(motionSensor, time, senderLogicalTime);
                        break;

                    case DOOR:
                        final DoorSensor doorSensor = ((DoorSensor) sensor);
                        dbServer.doorToggled(doorSensor, time, senderLogicalTime);
                        break;
                }
                break;

            case DEVICE:
                final Device device = ((Device) ioT);
                dbServer.deviceToggled(device, time, senderLogicalTime);
                break;
        }
    }

    @Override
    public void setDeviceState(final Device device, final boolean state) {
        incrementLogicalTime(0);

        try {
            DeviceServer.connect(getRegisteredIoTs().get(device)).setState(state, getLogicalTime());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<IoT, Address> fetchRegisteredIoTs() throws RemoteException {
        incrementLogicalTime(0);

        return getRegisteredIoTs();
    }

    @Override
    public void raiseAlarm() throws RemoteException {
        // TODO Perform necessary actions which should happen after an alarm has been raised
        System.out.println("An intruder has entered the house!!");
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
        incrementLogicalTime(0);

        // Send to DB
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.DB)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> {
                    try {
                        DbServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime());
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
                        SensorServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime());
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
                        DeviceServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private GatewayConfig getGatewayConfig() {
        return ((GatewayConfig) getConfig());
    }
}
