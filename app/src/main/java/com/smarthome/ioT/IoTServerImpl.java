package com.smarthome.ioT;

import com.smarthome.enums.IoTType;
import com.smarthome.enums.SensorType;
import com.smarthome.ioT.db.DbServer;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.ioT.sensor.SensorServer;
import com.smarthome.model.Address;
import com.smarthome.model.IoT;
import com.smarthome.model.config.ServerConfig;
import com.smarthome.model.sensor.Sensor;

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

public abstract class IoTServerImpl implements IoTServer {

    private final ServerConfig serverConfig;
    private final IoT ioT;
    private final Map<IoT, Long> offsetMap;
    private long logicalTime;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;

    /**
     * Creates an instance of an IoT using a provided config.
     * <p>
     * Additionally, registers itself remotely to the Gateway, if itself is not a Gateway.
     *
     * @param serverConfig              The config used to initialize its IoT server
     * @param registerRemotelyToGateway If {@code true} then register remotely to Gateway;
     *                                  else register locally to its contained
     *                                  {@link #registeredIoTs}.
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    protected IoTServerImpl(final ServerConfig serverConfig, final boolean registerRemotelyToGateway)
            throws RemoteException {
        this.serverConfig = serverConfig;
        ioT = createIoT();
        offsetMap = new HashMap<>();
        setLogicalTime(0);
        registeredIoTs = new HashMap<>();

        startServer(serverConfig.getAddress().getPortNo());

        if (registerRemotelyToGateway) {
            System.out.println("Registering to Gateway...");
            try {
                GatewayServer.connect(serverConfig.getGatewayAddress()).register(ioT, serverConfig.getAddress(),
                        getLogicalTime());
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
            System.out.println("Successfully registered.");
        } else {
            getRegisteredIoTs().put(getIoT(), getServerConfig().getAddress());
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
        UnicastRemoteObject.exportObject(this, portNo);
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(getName(), this);

        System.out.println("Server started.");
    }

    protected abstract String getName();

    protected ServerConfig getServerConfig() {
        return serverConfig;
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
        System.out.println("Received Time Synchronization offset of "
                + synchronizationOffset + " ms.");

        this.synchronizationOffset = synchronizationOffset;
    }

    @Override
    public long getCurrentTime() throws RemoteException {
        return System.currentTimeMillis() + getServerConfig().getRandomTimeOffset();
    }

    protected Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs,
            final long senderLogicalTime) throws RemoteException {
        incrementLogicalTime(senderLogicalTime);

        System.out.println("Received Map of Registered IoTs from Gateway.");

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

        final boolean leader = getIoT().getId().equals(uuidList.get(uuidList.size() - 1));

        if (leader) {
            System.out.println("I am the Leader.");
        }

        return leader;
    }

    /**
     * Synchronizes the time of all IoTs in this distributed system using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    protected void synchronizeTime() {
        System.out.println("Starting Time Synchronization...");

        buildOffsetMap();

        System.out.println("Time Synchronization Offset Map:");
        getOffsetMap().keySet()
                .forEach(ioT1 ->
                        System.out.printf("\t%-10s %-10s%n", ioT1, getOffsetMap().get(ioT1)));

        final long[] offsetTotal = {0};
        getOffsetMap().values().forEach(offset -> offsetTotal[0] += offset);

        sendSynchronizationOffsets(offsetTotal[0] / getOffsetMap().size());

        System.out.println("\nTime Synchronization complete.");
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
                getOffsetMap().get(getIoT()) - offsetAverage
        );

        // Send synchronization offset to Gateway Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.GATEWAY)
                .forEach(ioT -> new Thread(() -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        GatewayServer.connect(address).setSynchronizationOffset(
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());

        // Send synchronization offset to DB Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DB)
                .forEach(ioT -> new Thread(() -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        DbServer.connect(address).setSynchronizationOffset(
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());

        // Send synchronization offset to Sensor Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .forEach(ioT -> new Thread(() -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        SensorServer.connect(address).setSynchronizationOffset(
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());

        // Send synchronization offset to Device Servers
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DEVICE)
                .forEach(ioT -> new Thread(() -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        DeviceServer.connect(address).setSynchronizationOffset(
                                getOffsetMap().get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());
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

    /**
     * Checks whether a Presence Sensor located on another server is activated or not.
     *
     * @return {@code true} if the remote Presence Sensor is activated;
     * {@code false} otherwise
     */
    protected boolean isRemotePresenceSensorActivated() {
        final boolean[] authorizedUser = new boolean[1];

        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> ioT.getIoTType() == IoTType.SENSOR)
                .map(ioT -> ((Sensor) ioT))
                .filter(sensor -> sensor.getSensorType() == SensorType.PRESENCE)
                .map(sensor -> getRegisteredIoTs().get(sensor))
                .forEach((Address address) -> {
                    try {
                        authorizedUser[0] = SensorServer.connect(address)
                                .isPresenceSensorActivated(getLogicalTime());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });

        return authorizedUser[0];
    }

    protected void raiseRemoteAlarm() {
        System.out.println("Raising Alarm!");
        System.out.println("Informing Gateway...");

        try {
            GatewayServer.connect(getServerConfig().getGatewayAddress()).raiseAlarm(getLogicalTime());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
