package com.smarthome.ioT.db;

import com.smarthome.enums.IoTType;
import com.smarthome.ioT.IoTServerImpl;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.config.Config;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.rmi.RemoteException;
import java.util.UUID;

public class DbServerImpl extends IoTServerImpl implements DbServer {

    private final Logger logger;

    public DbServerImpl(final Config config) throws RemoteException {
        super(config, true);
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
        // TODO append to log
    }

    @Override
    public void motionDetected(final MotionSensor motionSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        // TODO append to log
    }

    @Override
    public void doorToggled(final DoorSensor doorSensor, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        // TODO append to log
    }

    @Override
    public void deviceToggled(final Device device, final long chronologicalTime,
            final long logicalTime) throws RemoteException {
        // TODO append to log
    }

    private Logger getLogger() {
        return logger;
    }
}
