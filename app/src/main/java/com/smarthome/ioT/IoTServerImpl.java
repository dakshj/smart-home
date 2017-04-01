package com.smarthome.ioT;

import com.smarthome.enums.IoTType;
import com.smarthome.ioT.db.DbServer;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.ioT.sensor.SensorServer;
import com.smarthome.model.Address;
import com.smarthome.model.IoT;
import com.smarthome.model.config.Config;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class IoTServerImpl extends UnicastRemoteObject implements IoTServer {

    private final Config config;
    private final IoT ioT;
    private final Map<IoT, Long> offsetMap;
    private long logicalTime;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;

    protected IoTServerImpl(final Config config, final boolean registerToGateway)
            throws RemoteException {
        this.config = config;
        ioT = createIoT();
        offsetMap = new HashMap<>();
        setLogicalTime(0);

        startServer(config.getAddress().getPortNo());

        if (registerToGateway) {
            try {
                GatewayServer.connect(config.getGatewayAddress()).register(ioT, config.getAddress(),
                        getLogicalTime());
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract IoT createIoT();

    /**
     * Starts the DB Server on the provided port number.
     *
     * @param portNo The port number to start the DB Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(getName(), this);
    }

    protected abstract String getName();

    @Override
    public Config getConfig() {
        return config;
    }

    protected IoT getIoT() {
        return ioT;
    }

    private Map<IoT, Long> getOffsetMap() {
        return offsetMap;
    }

    /**
     * Gets the logical time of this server, based on
     * <a href="https://en.wikipedia.org/wiki/Lamport_timestamps">Lamport timestamps</a>.
     *
     * @return The logical time of this server
     */
    protected long getLogicalTime() {
        return logicalTime;
    }

    private void setLogicalTime(final long logicalTime) {
        this.logicalTime = logicalTime;
    }

    protected void incrementLogicalTime(final long senderLogicalTime) {
        if (getLogicalTime() < senderLogicalTime) {
            setLogicalTime(senderLogicalTime);
        }

        setLogicalTime(getLogicalTime() + 1);
    }

    private long getSynchronizationOffset() {
        return synchronizationOffset;
    }

    @Override
    public void setSynchronizationOffset(final long synchronizationOffset) {
        this.synchronizationOffset = synchronizationOffset;
    }

    protected Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs,
            final long senderLogicalTime) throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

        this.registeredIoTs = registeredIoTs;
        if (isLeader()) {
            synchronizeTime();
        }
    }

    /**
     * Checks if the current IoT has the highest UUID among all registered IoTs.
     *
     * @return {@code true} if current IoT has the highest UUID;
     * {@code false} otherwise
     */
    protected boolean isLeader() {
        final List<UUID> uuidList = getRegisteredIoTs().keySet().stream()
                .map(IoT::getId)
                .collect(Collectors.toList());

        uuidList.sort(null);

        return getIoT().getId().equals(uuidList.get(uuidList.size() - 1));
    }

    /**
     * Synchronizes the time of all IoTs in this distributed system using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    protected void synchronizeTime() {
        buildOffsetMap();

        final long[] offsetTotal = {0};
        getOffsetMap().values().forEach(offset -> offsetTotal[0] += offset);

        sendSynchronizationOffsets(offsetTotal[0] / getOffsetMap().size());
    }

    /**
     * Builds a {@link Map} of all server times' offsets w.r.t. this server,
     * which will be later used to get the offset of the entire distributed system
     * and synchronize times using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    private void buildOffsetMap() {
        getOffsetMap().clear();

        // Put self's offset as 0
        getOffsetMap().put(getIoT(), 0L);

        // Calculate offset for Gateway Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.GATEWAY)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        final long requestTime = getCurrentTime();
                        final long remoteCurrentTime = GatewayServer.connect(address).getCurrentTime();
                        final long responseTime = getCurrentTime();

                        getOffsetMap().put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Calculate offset for DB Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DB)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        final long requestTime = getCurrentTime();
                        final long remoteCurrentTime = DbServer.connect(address).getCurrentTime();
                        final long responseTime = getCurrentTime();

                        getOffsetMap().put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Calculate offset for Sensor Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        final long requestTime = getCurrentTime();
                        final long remoteCurrentTime = SensorServer.connect(address).getCurrentTime();
                        final long responseTime = getCurrentTime();

                        getOffsetMap().put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Calculate offset for Device Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        final long requestTime = getCurrentTime();
                        final long remoteCurrentTime = DeviceServer.connect(address).getCurrentTime();
                        final long responseTime = getCurrentTime();

                        getOffsetMap().put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private long getOffset(final long requestTime, final long remoteCurrentTime,
            final long responseTime) {
        return requestTime - remoteCurrentTime + (responseTime - requestTime) / 2;
    }

    private void sendSynchronizationOffsets(final long offsetAverage) {
        // Send synchronization offset to self
        setSynchronizationOffset(
                // TODO confirm the below formula, if this or reverse of it
                getOffsetMap().get(getIoT()) - offsetAverage
        );

        // Send synchronization offset to Gateway Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.GATEWAY)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        GatewayServer.connect(address).setSynchronizationOffset(
                                // TODO confirm the below formula, if this or reverse of it
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Send synchronization offset to DB Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DB)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        DbServer.connect(address).setSynchronizationOffset(
                                // TODO confirm the below formula, if this or reverse of it
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Send synchronization offset to Sensor Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        SensorServer.connect(address).setSynchronizationOffset(
                                // TODO confirm the below formula, if this or reverse of it
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        // Send synchronization offset to Device Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        DeviceServer.connect(address).setSynchronizationOffset(
                                // TODO confirm the below formula, if this or reverse of it
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Returns the System's current time after adjustment by adding an offset,
     * calculated using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>
     * for clock synchronization.
     *
     * @return The offset-adjusted System time
     */
    protected long getSynchronizedTime() {
        try {
            return getCurrentTime() + getSynchronizationOffset();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
