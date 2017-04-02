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

import java.rmi.RemoteException;
import java.util.UUID;

public class DbServerImpl extends IoTServerImpl implements DbServer {

    private final Logger logger;

    public DbServerImpl(final ServerConfig serverConfig) throws RemoteException {
        super(serverConfig, true);
        logger = new Logger();
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
        getLogger().log(new Log(LogType.RAW, temperatureSensor.getId(),
                temperatureSensor.getIoTType(), temperatureSensor.getSensorType(), null,
                chronologicalTime, logicalTime,
                "Temperature changed to " + temperatureSensor.getData() + "."));
    }

    @Override
    public void motionDetected(final MotionSensor motionSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        getLogger().log(new Log(LogType.RAW, motionSensor.getId(), motionSensor.getIoTType(),
                motionSensor.getSensorType(), null, chronologicalTime, logicalTime,
                "Motion detected."));
    }

    @Override
    public void doorToggled(final DoorSensor doorSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        getLogger().log(new Log(LogType.RAW, doorSensor.getId(), doorSensor.getIoTType(),
                doorSensor.getSensorType(), null, chronologicalTime, logicalTime,
                "Door " + (doorSensor.getData() ? "opened" : "closed") + "."));
    }

    @Override
    public void deviceToggled(final Device device, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        getLogger().log(new Log(LogType.RAW, device.getId(), device.getIoTType(), null,
                device.getDeviceType(), chronologicalTime, logicalTime,
                device.getDeviceType() + " switched "
                        + (device.getState() ? "on" : "off") + "."));
    }

    private Logger getLogger() {
        return logger;
    }
}
