package com.smarthome.entrant;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.ioT.sensor.SensorServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.config.EntrantConfig;
import com.smarthome.model.sensor.Sensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Entrant {

    /**
     * The minimum time (in milliseconds) gap for randomly performing the next action.
     */
    private static final long TIME_DELAY_MIN = 500;

    /**
     * The maximum time (in milliseconds) gap for randomly performing the next action.
     */
    private static final long TIME_DELAY_MAX = 1500;

    private final EntrantConfig entrantConfig;

    private Map<IoT, Address> registeredIoTs;

    public Entrant(final EntrantConfig entrantConfig) throws RemoteException {
        this.entrantConfig = entrantConfig;

        try {
            System.out.println("Fetching the Map of all Registered IoTs from Gateway...");

            setRegisteredIoTs(GatewayServer.connect(entrantConfig.getGatewayAddress())
                    .fetchRegisteredIoTs());

            System.out.println("Successfully fetched.");
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        setPresenceSensorActivationStatus();

        performActions();
    }

    /**
     * Internal method which activates the Presence Sensor
     * if the current Entrant is an authorized user.
     */
    private void setPresenceSensorActivationStatus() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.PRESENCE)
                .map(sensor -> getRegisteredIoTs().get(sensor))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address)
                                .setPresenceServerActivated(getEntrantConfig().isAuthorized());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void performActions() {
        System.out.println("Opening the door.");
        setDoorSensors(true);

        System.out.println("Closing the door.");
        setDoorSensors(false);

        System.out.println("Moving around the Smart Home.");
        triggerMotionSensors();

        System.out.println("Randomly switching devices on and off, and randomly moving around.");
        randomlyToggleDevicesAndTriggerMotionSensors();

        System.out.println("Moving towards the door to leave the Smart Home.");
        triggerMotionSensors();

        if (getEntrantConfig().isAuthorized()) {
            System.out.println("Opening the door.");
            setDoorSensors(true);

            System.out.println("Closing the door.");
            setDoorSensors(false);
        } else {
            System.out.println("Cannot use the door because the Entrant is an Intruder!");
        }

        try {
            GatewayServer.connect(getEntrantConfig().getGatewayAddress()).entrantExecutionFinished();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param opened {@code true} of the door(s) need to be opened;
     *               {@code false} otherwise
     */
    private void setDoorSensors(final boolean opened) {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.DOOR)
                .map(doorSensor -> getRegisteredIoTs().get(doorSensor))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address).openOrCloseDoor(opened, 0);
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void triggerMotionSensors() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.MOTION)
                .map(motionSensor -> getRegisteredIoTs().get(motionSensor))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address).triggerMotionSensor();
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Randomly selects a few devices and toggles their status.
     * <p>
     * Additionally, randomly trigger motion sensors with a 50% probability.
     * <p>
     * Finally, randomly recursively calls self with a 80% probability.
     */
    private void randomlyToggleDevicesAndTriggerMotionSensors() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)

                // Adds randomness to the device toggling action
                .filter(ioT -> ThreadLocalRandom.current().nextBoolean())

                .map(ioT -> ((Device) ioT))
                .forEach(device -> {
                    addRandomDelay();

                    if (getEntrantConfig().isAuthorized()) {
                        System.out.println("Toggling the " + device + ".");

                        final Address address = getRegisteredIoTs().get(device);

                        try {
                            DeviceServer.connect(address).toggleState(0);
                        } catch (RemoteException | NotBoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Cannot toggle the " + device + " because" +
                                " the Entrant is an Intruder!");
                    }
                });

        addRandomDelay();

        if (ThreadLocalRandom.current().nextBoolean()) {
            System.out.println("Moving around the Smart Home.");
            triggerMotionSensors();
        }

        addRandomDelay();

        // Randomly recursively calls self with a 80% probability
        if (ThreadLocalRandom.current().nextInt(1, 101) <= 80) {
            randomlyToggleDevicesAndTriggerMotionSensors();
        }
    }

    private void addRandomDelay() {
        final long randomDelay = ThreadLocalRandom.current().nextLong(
                TIME_DELAY_MIN,
                TIME_DELAY_MAX
        );

        try {
            Thread.sleep(randomDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private EntrantConfig getEntrantConfig() {
        return entrantConfig;
    }

    private Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    private void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) {
        this.registeredIoTs = registeredIoTs;
    }
}
