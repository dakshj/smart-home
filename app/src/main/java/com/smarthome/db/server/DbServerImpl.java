package com.smarthome.db.server;

import com.smarthome.enums.IoTType;
import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.IoT;
import com.smarthome.model.config.DbConfig;
import com.smarthome.model.sensor.DoorSensor;
import com.smarthome.model.sensor.MotionSensor;
import com.smarthome.model.sensor.TemperatureSensor;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;

public class DbServerImpl extends UnicastRemoteObject implements DbServer {

    private final DbConfig dbConfig;
    private final IoT ioT;
    private final Logger logger;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;

    public DbServerImpl(final DbConfig dbConfig) throws RemoteException {
        this.dbConfig = dbConfig;
        ioT = new IoT(UUID.randomUUID(), IoTType.DB);
        logger = new Logger();

        startServer(dbConfig.getAddress().getPortNo());

        try {
            GatewayServer.connect(dbConfig.getGatewayAddress()).register(ioT, dbConfig.getAddress());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the DB Server on the provided port number.
     * <p>
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the DB Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    @Override
    public void temperatureChanged(final TemperatureSensor temperatureSensor, final long time)
            throws RemoteException {
        // TODO append to log
    }

    @Override
    public void motionDetected(final MotionSensor motionSensor, final long time)
            throws RemoteException {
        // TODO append to log
    }

    @Override
    public void doorToggled(final DoorSensor doorSensor, final long time) throws RemoteException {
        // TODO append to log
    }

    @Override
    public void deviceToggled(final Device device, final long time) throws RemoteException {
        // TODO append to log
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) throws RemoteException {
        this.registeredIoTs = registeredIoTs;
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

    private IoT getIoT() {
        return ioT;
    }

    private DbConfig getDbConfig() {
        return dbConfig;
    }

    private Logger getLogger() {
        return logger;
    }
}
