package com.smarthome.sensor.server;

import com.smarthome.enums.SensorType;
import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

public class SensorServerImpl extends UnicastRemoteObject implements SensorServer {

    private final Address gatewayAddress;

    private Sensor sensor;
    private long synchronizationOffset;

    public SensorServerImpl(final Sensor sensor, final Address selfAddress,
            final Address gatewayAddress) throws RemoteException {
        this.sensor = sensor;
        this.gatewayAddress = gatewayAddress;

        startServer(selfAddress.getPortNo());

        try {
            setSensor((Sensor) GatewayServer.connect(gatewayAddress).register(sensor, selfAddress));
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
    public void queryState() throws RemoteException {
        try {
            GatewayServer.connect(gatewayAddress).reportState(getSensor(), getSynchronizedTime());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private Sensor getSensor() {
        return sensor;
    }

    private void setSensor(final Sensor sensor) {
        this.sensor = sensor;
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
