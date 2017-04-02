package com.smarthome.ioT.gateway;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class GatewayServerImpl extends IoTServerImpl implements GatewayServer {

    private static final long TIME_RESYNC_DELAY = 30 * 1000;

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
        getRegisteredIoTs().put(ioT, address);
    }

    /**
     * Queries the current state of the IoT.
     *
     * @param ioT The identifier of that ioT
     */
    private void queryState(final IoT ioT) throws RemoteException {
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

    /**
     * Queries the states of all sensors and devices.
     */
    private void queryStates() {
        System.out.println("Querying the current state of all Sensors and Devices...");

        getRegisteredIoTs().keySet().forEach(ioT -> {
            try {
                queryState(ioT);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
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
                        System.out.println("State of " + temperatureSensor + " : "
                                + temperatureSensor.getData() + "°F.");
                        dbServer.temperatureChanged(temperatureSensor, time, senderLogicalTime);
                        break;

                    case MOTION:
                        final MotionSensor motionSensor = ((MotionSensor) sensor);
                        dbServer.motionDetected(motionSensor, time, senderLogicalTime);
                        break;

                    case DOOR:
                        final DoorSensor doorSensor = ((DoorSensor) sensor);
                        System.out.println("State of " + doorSensor + " : "
                                + (doorSensor.getData() ? "Open" : "Closed") + ".");
                        dbServer.doorToggled(doorSensor, time, senderLogicalTime);
                        break;
                }
                break;

            case DEVICE:
                final Device device = ((Device) ioT);
                System.out.println("State of " + device + " : "
                        + (device.getState() ? "On" : "Off") + ".");
                dbServer.deviceToggled(device, time, senderLogicalTime);
                break;
        }
    }

    /**
     * Sets the state of the device.
     *
     * @param device The Device whose state needs to be changed
     * @param state  The new state of the device
     */
    private void setDeviceState(final Device device, final boolean state) {
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
    public void raiseAlarm(final long senderLogicalTime) throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

        System.out.println("An intruder has entered the Smart Home!");
        switchAllOutlets(false);
        switchAllBulbs(true);
        System.out.println("Contacting 911 and the Home Owner!");
    }

    private void switchAllOutlets(final boolean status) {
        System.out.println("Switching " + (status ? "on" : "off") + " all outlets!");

        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .map(ioT -> ((Device) ioT))
                .filter(device -> device.getDeviceType() == DeviceType.OUTLET)
                .forEach(device -> setDeviceState(device, status));
    }

    private void switchAllBulbs(final boolean status) {
        System.out.println("Switching " + (status ? "on" : "off") + " all bulbs!");

        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .map(ioT -> ((Device) ioT))
                .filter(device -> device.getDeviceType() == DeviceType.BULB)
                .forEach(device -> setDeviceState(device, status));
    }

    private void waitForUserToStartLeaderElectionAndTimeSync() {
        System.out.println("\nPlease press Enter after all IoT servers are running.\n" +
                "Pressing Enter will begin the Leader Election and Time Synchronization jobs.");
        new Scanner(System.in).next();

        periodicallyElectLeaderAndSynchronizeClocks();
    }

    /**
     * Performs {@link #electLeaderAndSynchronizeClocks()} and sets a {@value TIME_RESYNC_DELAY} ms
     * Timer to rerun the method.
     */
    private void periodicallyElectLeaderAndSynchronizeClocks() {
        electLeaderAndSynchronizeClocks();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n~~~~~~Resynchronizing Times~~~~~~");
                periodicallyElectLeaderAndSynchronizeClocks();
                System.out.println("\n~~~~~~Resynchronization complete~~~~~~\n");
            }
        }, TIME_RESYNC_DELAY);
    }

    /**
     * Elects a leader using the
     * <a href="https://en.wikipedia.org/wiki/Bully_algorithm">Bully algorithm</a>.
     * <p>
     * Additionally, initiates Clock Synchronization in the chosen leader.
     */
    private void electLeaderAndSynchronizeClocks() {
        System.out.println("\nElecting a Leader for Time Synchronization...");
        if (isLeader()) {
            synchronizeTime();
        } else {
            System.out.println("Broadcasting Map of Registered IoTs, to all IoTs...");
            broadcastRegisteredIoTs();
            System.out.println("Broadcast complete.");
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
        return ((GatewayConfig) getServerConfig());
    }

    private void userAtHome(final boolean atHome) {
        System.out.println("Switched to " + (atHome ? "HOME" : "AWAY") + " mode.");

        queryStates();

        switchAllBulbs(atHome);

        if (!atHome) {
            switchAllOutlets(false);

            getRegisteredIoTs().keySet().stream()
                    .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                    .map(ioT -> ((Sensor) ioT))
                    .filter(sensor -> sensor.getSensorType() == SensorType.PRESENCE)
                    .map(sensor -> getRegisteredIoTs().get(sensor))
                    .forEach(address -> {
                        try {
                            SensorServer.connect(address).setPresenceServerActivated(false);
                        } catch (RemoteException | NotBoundException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
