package com.smarthome.entrant.server;

import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.Entrant;
import com.smarthome.model.IoT;
import com.smarthome.model.config.EntrantConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.ioT.sensor.SensorServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class EntrantServerImpl extends UnicastRemoteObject implements EntrantServer {

    private final EntrantConfig entrantConfig;
    private final Entrant entrant;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;

    public EntrantServerImpl(final EntrantConfig entrantConfig) throws RemoteException {
        this.entrantConfig = entrantConfig;
        entrant = new Entrant(entrantConfig.getEntrantType());

        try {
            setRegisteredIoTs(GatewayServer.connect(entrantConfig.getGatewayAddress())
                    .fetchRegisteredIoTs());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        triggerIoTs();
    }

    private void triggerIoTs() {
        triggerMotionSensors();
        toggleDoorSensors();
        toggleDevices();
    }

    private void triggerMotionSensors() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.MOTION)
                .map(sensor -> ((MotionSensor) sensor))
                .map(motionSensor -> getRegisteredIoTs().get(motionSensor))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address).triggerMotionSensor();
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void toggleDoorSensors() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.DOOR)
                .map(sensor -> ((DoorSensor) sensor))
                .map(doorSensor -> getRegisteredIoTs().get(doorSensor))
                .forEach(address -> {
                    try {
                        SensorServer.connect(address).toggleDoorSensor();
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void toggleDevices() {
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .map(ioT -> ((Device) ioT))
                .map(device -> getRegisteredIoTs().get(device))
                .forEach(address -> {
                    try {
                        DeviceServer.connect(address).toggleState(0);
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * <p>
     * Starts the Entrant Server on the provided port number.
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the Entrant Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    private EntrantConfig getEntrantConfig() {
        return entrantConfig;
    }

    private Entrant getEntrant() {
        return entrant;
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

    private Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    private void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) {
        this.registeredIoTs = registeredIoTs;
    }
}
