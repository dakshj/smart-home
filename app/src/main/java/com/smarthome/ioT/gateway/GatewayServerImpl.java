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
import com.smarthome.model.Log;
import com.smarthome.model.config.GatewayConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;
import com.smarthome.util.LimitedSizeArrayList;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class GatewayServerImpl extends IoTServerImpl implements GatewayServer {

    private boolean alreadyRaisedAlarm;

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
            final long senderLogicalTime, final UUID senderId) throws RemoteException {
        // No-op
    }

    @Override
    public void register(final IoT ioT, final Address address, final long senderLogicalTime,
            final UUID senderId)
            throws RemoteException {
        incrementLogicalTime(senderLogicalTime, senderId);
        getRegisteredIoTs().put(ioT, address);
    }

    /**
     * Queries the current state of the IoT.
     *
     * @param ioT The identifier of that ioT
     */
    private void queryState(final IoT ioT) throws RemoteException {
        incrementLogicalTime(0, null);

        if (getRegisteredIoTs().containsKey(ioT)) {
            try {
                switch (ioT.getIoTType()) {
                    case SENSOR:
                        SensorServer.connect(getRegisteredIoTs().get(ioT))
                                .queryState(getLogicalTime(), getIoT().getId());
                        break;

                    case DEVICE:
                        DeviceServer.connect(getRegisteredIoTs().get(ioT))
                                .queryState(getLogicalTime(), getIoT().getId());
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

        getRegisteredIoTs().keySet().forEach(ioT ->
                new Thread(() -> {
                    try {
                        queryState(ioT);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }).start());
    }

    @Override
    public void reportState(final IoT ioT, final long time, final long senderLogicalTime,
            final UUID senderId) throws RemoteException {
        incrementLogicalTime(senderLogicalTime, senderId);

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

                    {
                        final LimitedSizeArrayList<Log> youngestLogsList =
                                dbServer.getYoungestLogsList();

                        if (youngestLogsList.getEldest().getIoTType() != null &&
                                youngestLogsList.getEldest().getIoTType() == IoTType.SENSOR &&
                                youngestLogsList.getEldest().getSensorType() != null &&
                                youngestLogsList.getEldest().getSensorType() == SensorType.DOOR) {
                            someoneEnteredHome(true);
                        }
                    }
                    break;

                    case DOOR:
                        final DoorSensor doorSensor = ((DoorSensor) sensor);
                        System.out.println("State of " + doorSensor + " : "
                                + (doorSensor.getData() ? "Open" : "Closed") + ".");
                        dbServer.doorToggled(doorSensor, time, senderLogicalTime);

                    {
                        final LimitedSizeArrayList<Log> youngestLogsList =
                                dbServer.getYoungestLogsList();

                        if (youngestLogsList.getEldest().getIoTType() != null &&
                                youngestLogsList.getEldest().getIoTType() == IoTType.SENSOR &&
                                youngestLogsList.getEldest().getSensorType() != null &&
                                youngestLogsList.getEldest().getSensorType() == SensorType.MOTION) {
                            someoneEnteredHome(false);
                        }
                    }
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
        incrementLogicalTime(0, null);

        try {
            DeviceServer.connect(getRegisteredIoTs().get(device))
                    .setState(state, getLogicalTime(), getIoT().getId());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<IoT, Address> fetchRegisteredIoTs() throws RemoteException {
        incrementLogicalTime(0, null);

        return getRegisteredIoTs();
    }

    @Override
    public void raiseAlarm(final long senderLogicalTime, final UUID senderId) throws RemoteException {
        incrementLogicalTime(senderLogicalTime, senderId);

        if (alreadyRaisedAlarm) {
            return;
        }

        alreadyRaisedAlarm = true;

        System.out.println("An intruder has entered the Smart Home!");
        switchOffAllOutlets();
        switchAllBulbs(true);
        System.out.println("Contacting 911 and the Home Owner!");
    }

    @Override
    public void entrantExecutionFinished() throws RemoteException {
        incrementLogicalTime(0, null);
        alreadyRaisedAlarm = false;
    }

    private void switchOffAllOutlets() {
        System.out.println("Switching off all outlets!");

        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .map(ioT -> ((Device) ioT))
                .filter(device -> device.getDeviceType() == DeviceType.OUTLET)
                .forEach(device -> new Thread(() -> setDeviceState(device, false)).start());
    }

    private void switchAllBulbs(final boolean status) {
        System.out.println("Switching " + (status ? "on" : "off") + " all bulbs!");

        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .map(ioT -> ((Device) ioT))
                .filter(device -> device.getDeviceType() == DeviceType.BULB)
                .forEach(device -> new Thread(() -> setDeviceState(device, status)).start());
    }

    private void waitForUserToStartLeaderElectionAndTimeSync() {
        System.out.println("\nPlease input any character after all IoT servers are running.\n" +
                "This will begin the Leader Election and Time Synchronization jobs.");
        new Scanner(System.in).next();

        periodicallyElectLeaderAndSynchronizeClocks();
    }

    private GatewayConfig getGatewayConfig() {
        return ((GatewayConfig) getServerConfig());
    }

    private void someoneEnteredHome(final boolean atHome) {
        if (!isRemotePresenceSensorActivated()) {
            try {
                DbServer.connect(getGatewayConfig().getDbAddress())
                        .intruderEntered(getSynchronizedTime(), getLogicalTime());
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            DbServer.connect(getGatewayConfig().getDbAddress())
                    .userEntered(atHome, getSynchronizedTime(), getLogicalTime());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        System.out.println("User " + (atHome ? "entered" : "exited") + " the Smart Home.");

        System.out.println("Switched to " + (atHome ? "HOME" : "AWAY") + " mode.");

        switchAllBulbs(atHome);

        if (!atHome) {
            switchOffAllOutlets();
            resetAllPresenceSensorsToInactive();
        }
    }

    private void resetAllPresenceSensorsToInactive() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.PRESENCE)
                .map(sensor -> getRegisteredIoTs().get(sensor))
                .forEach(address -> new Thread(() -> {
                    try {
                        SensorServer.connect(address).setPresenceServerActivated(false);
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());
    }
}
