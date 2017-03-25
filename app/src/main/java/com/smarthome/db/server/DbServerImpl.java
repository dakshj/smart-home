package com.smarthome.db.server;

import com.smarthome.model.DoorSensor;
import com.smarthome.model.MotionSensor;
import com.smarthome.model.TemperatureSensor;

import java.rmi.RemoteException;

public class DbServerImpl implements DbServer {

    @Override
    public void temperatureChanged(final TemperatureSensor temperatureSensor, final long time) throws RemoteException {

    }

    @Override
    public void motionDetected(final MotionSensor motionSensor, final long time) throws RemoteException {

    }

    @Override
    public void doorToggled(final DoorSensor doorSensor, final long time) throws RemoteException {

    }
}
