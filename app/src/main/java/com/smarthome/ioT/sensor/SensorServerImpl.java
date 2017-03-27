package com.smarthome.ioT.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.IoT;
import com.smarthome.model.config.SensorConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SensorServerImpl extends UnicastRemoteObject implements SensorServer {

    private final SensorConfig sensorConfig;
    private final Sensor sensor;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;

    public SensorServerImpl(final SensorConfig sensorConfig) throws RemoteException {
        this.sensorConfig = sensorConfig;
        sensor = new Sensor(UUID.randomUUID(), IoTType.SENSOR, sensorConfig.getSensorType());

        startServer(sensorConfig.getAddress().getPortNo());

        try {
            GatewayServer.connect(sensorConfig.getGatewayAddress())
                    .register(sensor, sensorConfig.getAddress());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        if (getSensor().getSensorType() == SensorType.TEMPERATURE) {
            periodicallyGenerateTemperatureValues();
        }
    }

    /**
     * Generates a random Temperature value (in °F).
     * <p>
     * Next, waits for a random duration.
     * <p>
     * Finally, repeats the above.
     */
    private void periodicallyGenerateTemperatureValues() {
        final long delayForNextValueGeneration = ThreadLocalRandom.current().nextLong(
                TemperatureSensor.VALUE_GENERATION_GAP_MIN,
                TemperatureSensor.VALUE_GENERATION_GAP_MAX
        );

        final double nextTemp = ThreadLocalRandom.current().nextDouble(
                TemperatureSensor.VALUE_MIN,
                TemperatureSensor.VALUE_MAX
        );

        ((TemperatureSensor) getSensor()).setData(nextTemp);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                periodicallyGenerateTemperatureValues();
            }
        }, delayForNextValueGeneration);
    }

    /**
     * Starts the Sensor Server on the provided port number.
     * <p>
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the Sensor Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    @Override
    public IoT getIoT() {
        return getSensor();
    }

    @Override
    public Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    @Override
    public void queryState() throws RemoteException {
        try {
            GatewayServer.connect(getSensorConfig().getGatewayAddress())
                    .reportState(getSensor(), getSynchronizedTime());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void triggerMotionSensor() throws RemoteException {
        if (getSensor().getSensorType() != SensorType.MOTION) {
            return;
        }

        queryState();
    }

    @Override
    public void toggleDoorSensor() throws RemoteException {
        if (getSensor().getSensorType() != SensorType.DOOR) {
            return;
        }

        final DoorSensor doorSensor = ((DoorSensor) getSensor());
        doorSensor.setData(doorSensor.getData());

        queryState();
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) throws RemoteException {
        this.registeredIoTs = registeredIoTs;
        if (isLeader()) {
            synchronizeTime();
        }
    }

    @Override
    public long getSynchronizationOffset() {
        return synchronizationOffset;
    }

    @Override
    public void setSynchronizationOffset(final long synchronizationOffset) throws RemoteException {
        this.synchronizationOffset = synchronizationOffset;
    }

    private SensorConfig getSensorConfig() {
        return sensorConfig;
    }

    private Sensor getSensor() {
        return sensor;
    }
}
