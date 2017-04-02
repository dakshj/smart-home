package com.smarthome.ioT.db;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.LogType;
import com.smarthome.ioT.IoTServerImpl;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.Log;
import com.smarthome.model.config.ServerConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.TemperatureSensor;
import com.smarthome.util.LimitedSizeArrayList;

import java.rmi.RemoteException;
import java.util.UUID;

public class DbServerImpl extends IoTServerImpl implements DbServer {

    private static final int YOUNGEST_LOGS_LIST_SIZE = 2;

    private final Logger logger;
    private final LimitedSizeArrayList<Log> youngestLogsList;

    public DbServerImpl(final ServerConfig serverConfig) throws RemoteException {
        super(serverConfig, true);
        logger = new Logger(getCurrentTime());
        youngestLogsList = new LimitedSizeArrayList<>(YOUNGEST_LOGS_LIST_SIZE);
    }

    @Override
    public IoT createIoT() {
        return new IoT(UUID.randomUUID(), IoTType.DB);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void temperatureChanged(final TemperatureSensor temperatureSensor,
            final long chronologicalTime, final long logicalTime) throws RemoteException {
        final Log log = new Log(LogType.RAW, temperatureSensor.getId(),
                temperatureSensor.getIoTType(), temperatureSensor.getSensorType(), null,
                chronologicalTime, logicalTime,
                "Temperature changed to " + temperatureSensor.getData() + ".");

        getYoungestLogsList().add(log);

        getLogger().log(log);
    }

    @Override
    public void motionDetected(final MotionSensor motionSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        final Log log = new Log(LogType.RAW, motionSensor.getId(), motionSensor.getIoTType(),
                motionSensor.getSensorType(), null, chronologicalTime, logicalTime,
                "Motion detected.");

        getYoungestLogsList().add(log);

        getLogger().log(log);
    }

    @Override
    public void doorToggled(final DoorSensor doorSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        final Log log = new Log(LogType.RAW, doorSensor.getId(), doorSensor.getIoTType(),
                doorSensor.getSensorType(), null, chronologicalTime, logicalTime,
                "Door " + (doorSensor.getData() ? "opened" : "closed") + ".");

        getYoungestLogsList().add(log);

        getLogger().log(log);
    }

    @Override
    public void deviceToggled(final Device device, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        final Log log = new Log(LogType.RAW, device.getId(), device.getIoTType(), null,
                device.getDeviceType(), chronologicalTime, logicalTime,
                device.getDeviceType() + " switched "
                        + (device.getState() ? "on" : "off") + ".");

        getYoungestLogsList().add(log);

        getLogger().log(log);
    }

    @Override
    public LimitedSizeArrayList<Log> getYoungestLogsList() throws RemoteException {
        return youngestLogsList;
    }

    @Override
    public void intruderEntered(final long chronologicalTime, final long logicalTime)
            throws RemoteException {
        getLogger().log(new Log(LogType.INFERRED, null, null, null, null,
                chronologicalTime, logicalTime, "Intruder entered the Smart Home."));
    }

    @Override
    public void userEntered(final boolean atHome, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        getLogger().log(new Log(LogType.INFERRED, null, null, null, null,
                chronologicalTime, logicalTime,
                "User " + (atHome ? "entered" : "exited") + " the Smart Home."));
    }

    private Logger getLogger() {
        return logger;
    }
}
