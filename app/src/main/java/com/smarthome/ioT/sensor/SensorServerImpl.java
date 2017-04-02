package com.smarthome.ioT.sensor;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
import com.smarthome.ioT.IoTServerImpl;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.model.IoT;
import com.smarthome.model.config.SensorConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.PresenceSensor;
import com.smarthome.model.sensor.Sensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.math.BigDecimal;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SensorServerImpl extends IoTServerImpl implements SensorServer {

    private final SensorConfig sensorConfig;

    public SensorServerImpl(final SensorConfig sensorConfig) throws RemoteException {
        super(sensorConfig, true);
        this.sensorConfig = sensorConfig;

        if (getSensor().getSensorType() == SensorType.TEMPERATURE) {
            periodicallyGenerateTemperatureValues();
        }
    }

    @Override
    public IoT createIoT() {
        switch (getSensorConfig().getSensorType()) {
            case TEMPERATURE:
                return new TemperatureSensor(UUID.randomUUID(), IoTType.SENSOR,
                        getSensorConfig().getSensorType());

            case MOTION:
                return new MotionSensor(UUID.randomUUID(), IoTType.SENSOR,
                        getSensorConfig().getSensorType());

            case DOOR:
                return new DoorSensor(UUID.randomUUID(), IoTType.SENSOR,
                        getSensorConfig().getSensorType());

            case PRESENCE:
                return new PresenceSensor(UUID.randomUUID(), IoTType.SENSOR,
                        getSensorConfig().getSensorType());
        }

        return null;
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void queryState(final long senderLogicalTime) throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

        try {
            GatewayServer.connect(getSensorConfig().getGatewayAddress())
                    .reportState(getSensor(), getSynchronizedTime(), getLogicalTime());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void triggerMotionSensor() throws RemoteException {
        if (getSensor().getSensorType() != SensorType.MOTION) {
            return;
        }

        System.out.println("Detected motion.");

        queryState(getLogicalTime());

        if (!isRemotePresenceSensorActivated()) {
            raiseRemoteAlarm();
        }
    }

    @Override
    public void openOrCloseDoor(final boolean opened, final long senderLogicalTime)
            throws RemoteException {
        if (getSensor().getSensorType() != SensorType.DOOR) {
            return;
        }

        incrementLogicalTime(senderLogicalTime);

        System.out.println("Door " + (opened ? "opened" : "closed") + ".");

        final DoorSensor doorSensor = ((DoorSensor) getSensor());
        doorSensor.setData(opened);

        queryState(getLogicalTime());
    }

    @Override
    public void setPresenceServerActivated(final boolean entrantAuthorized) throws RemoteException {
        if (getSensor().getSensorType() != SensorType.PRESENCE) {
            return;
        }

        PresenceSensor presenceSensor = ((PresenceSensor) getSensor());
        presenceSensor.setData(entrantAuthorized);
    }

    @Override
    public boolean isPresenceSensorActivated(final long senderLogicalTime) throws RemoteException {
        if (getSensor().getSensorType() != SensorType.PRESENCE) {
            return false;
        }

        incrementLogicalTime(senderLogicalTime);

        PresenceSensor presenceSensor = ((PresenceSensor) getSensor());
        return presenceSensor.getData();
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

        double nextTemp = ThreadLocalRandom.current().nextDouble(
                TemperatureSensor.VALUE_MIN,
                TemperatureSensor.VALUE_MAX
        );

        nextTemp = new BigDecimal(nextTemp)
                .setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();

        System.out.println("New Temperature: " + nextTemp + "°F.");

        //noinspection RedundantCast
        ((TemperatureSensor) getSensor()).setData(nextTemp);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                periodicallyGenerateTemperatureValues();
            }
        }, delayForNextValueGeneration);
    }

    private SensorConfig getSensorConfig() {
        return ((SensorConfig) getServerConfig());
    }

    private Sensor getSensor() {
        return ((Sensor) getIoT());
    }
}
