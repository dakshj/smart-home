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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class IoTServerImpl implements IoTServer {

    private static final long TIME_RESYNCHRONIZATION_DELAY = 30 * 1000;
    private static final long TIME_SYNCHRONIZATION_TIMEOUT = 3 * 1000;

    private final ServerConfig serverConfig;
    private final IoT ioT;
    private final Map<IoT, Long> offsetMap;
    private long logicalTime;

    private long synchronizationOffset;
    private Map<IoT, Address> registeredIoTs;
    private boolean timeSynchronizationSuccessful;

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
                GatewayServer.connect(serverConfig.getGatewayAddress())
                        .register(ioT, serverConfig.getAddress(), getLogicalTime(), getIoT().getId());
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

    /**
     * If the sender's logical time is greater than the receiver's logical time,
     * then the receiver's logical time is set to the sender's logical time.
     * <p>
     * If both are same, then the receiver's logical time is set to the logical time of
     * the one whose {@link IoT#id} is higher. Thus, the id of an IoT is used as a tie-breaker.
     * <p>
     * Finally, the receiver's logical time is incremented.
     *
     * @param senderLogicalTime The logical time of the calling IoT server
     * @param senderId          The {@link IoT#id} of the calling IoT server
     */
    protected void incrementLogicalTime(final long senderLogicalTime, final UUID senderId) {
        if (getLogicalTime() < senderLogicalTime) {
            setLogicalTime(senderLogicalTime);
        } else if (getLogicalTime() == senderLogicalTime && senderId != null) {
            if (getIoT().getId().compareTo(senderId) < 0) {
                setLogicalTime(senderLogicalTime);
            }
        }

        setLogicalTime(getLogicalTime() + 1);
    }

    private long getSynchronizationOffset() {
        return synchronizationOffset;
    }

    @Override
    public void setSynchronizationOffset(final long synchronizationOffset,
            final long senderLogicalTime, final UUID senderId) throws RemoteException {
        incrementLogicalTime(senderLogicalTime, senderId);

        System.out.println("Received Time Synchronization offset of "
                + synchronizationOffset + " ms.");

        this.synchronizationOffset = synchronizationOffset;

        if (getIoT().getIoTType() == IoTType.GATEWAY) {
            System.out.println("\n~~~~~~Time Synchronization Complete~~~~~~\n");
        }
    }

    @Override
    public long getCurrentTime() throws RemoteException {
        return System.currentTimeMillis() + getServerConfig().getRandomTimeOffset();
    }

    @Override
    public void leaderElected() throws RemoteException {
        System.out.println("Received response from Leader.");
        setTimeSynchronizationSuccessful(true);
    }

    protected Map<IoT, Address> getRegisteredIoTs() {
        return registeredIoTs;
    }

    @Override
    public void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs,
            final long senderLogicalTime, final UUID senderId) throws RemoteException {
        incrementLogicalTime(senderLogicalTime, senderId);

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
    private boolean isLeader() {
        final List<UUID> uuidList = getRegisteredIoTs().keySet().stream()
                .map(IoT::getId)
                .collect(Collectors.toList());

        uuidList.sort(null);

        final boolean leader = getIoT().getId().equals(uuidList.get(uuidList.size() - 1));

        if (leader) {
            System.out.println("I am the Leader.");

            if (getIoT().getIoTType() != IoTType.GATEWAY) {
                try {
                    GatewayServer.connect(getServerConfig().getGatewayAddress()).leaderElected();
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return leader;
    }

    /**
     * Synchronizes the time of all IoTs in this distributed system using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    private void synchronizeTime() {
        System.out.println("Starting Time Synchronization...");

        buildOffsetMap();

        System.out.println("Time Synchronization Offset Map:");
        getOffsetMap().keySet()
                .forEach(ioT1 ->
                        System.out.printf("\t%-10s %-10s%n", ioT1, getOffsetMap().get(ioT1)));

        final long[] offsetTotal = {0};
        getOffsetMap().values().forEach(offset -> offsetTotal[0] += offset);

        try {
            sendSynchronizationOffsets(offsetTotal[0] / getOffsetMap().size());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

    private void sendSynchronizationOffsets(final long offsetAverage) throws RemoteException {
        // Send synchronization offset to self
        setSynchronizationOffset(
                getOffsetMap().get(getIoT()) - offsetAverage, 0, null
        );

        // Send synchronization offset to Gateway Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.GATEWAY)
                .forEach(ioT -> new Thread(() -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        GatewayServer.connect(address).setSynchronizationOffset(
                                getOffsetMap().get(ioT) - offsetAverage,
                                getLogicalTime(), getIoT().getId()
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
                                getOffsetMap().get(ioT) - offsetAverage, getLogicalTime(),
                                getIoT().getId()
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
                                getOffsetMap().get(ioT) - offsetAverage, getLogicalTime(),
                                getIoT().getId()
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
                                getOffsetMap().get(ioT) - offsetAverage, getLogicalTime(),
                                getIoT().getId()
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
                                .isPresenceSensorActivated(getLogicalTime(), getIoT().getId());
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
            GatewayServer.connect(getServerConfig().getGatewayAddress())
                    .raiseAlarm(getLogicalTime(), getIoT().getId());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs {@link #electLeaderAndSynchronizeClocks()} and sets a {@value TIME_RESYNCHRONIZATION_DELAY} ms
     * Timer to rerun the method.
     */
    protected void periodicallyElectLeaderAndSynchronizeClocks() {
        electLeaderAndSynchronizeClocks();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                periodicallyElectLeaderAndSynchronizeClocks();
            }
        }, TIME_RESYNCHRONIZATION_DELAY);
    }

    /**
     * Elects a leader using the
     * <a href="https://en.wikipedia.org/wiki/Bully_algorithm">Bully algorithm</a>.
     * <p>
     * Additionally, initiates Clock Synchronization in the chosen leader.
     */
    private void electLeaderAndSynchronizeClocks() {
        System.out.println("\n~~~~~~Time Synchronization Started~~~~~~");

        System.out.println("\nElecting a Leader for Time Synchronization...");
        if (isLeader()) {
            synchronizeTime();
        } else {
            System.out.println("Broadcasting Map of Registered IoTs, to all IoTs...");
            broadcastRegisteredIoTs();
            System.out.println("Broadcast complete.");
            waitForResponseFromElectedLeader();
        }
    }

    /**
     * Broadcasts the {@link Map} of all registered IoTs to each IoT.
     */
    private void broadcastRegisteredIoTs() {
        incrementLogicalTime(0, null);

        // Send to DB
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.DB)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> new Thread(() -> {
                    try {
                        DbServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime(), getIoT().getId());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());

        // Send to all Sensors
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.SENSOR)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> new Thread(() -> {
                    try {
                        SensorServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime(), getIoT().getId());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());

        // Send to all Devices
        getRegisteredIoTs().keySet().stream()
                .filter(ioT1 -> ioT1.getIoTType() == IoTType.DEVICE)
                .map(ioT1 -> getRegisteredIoTs().get(ioT1))
                .forEach(address -> new Thread(() -> {
                    try {
                        DeviceServer.connect(address).setRegisteredIoTs(getRegisteredIoTs(),
                                getLogicalTime(), getIoT().getId());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }).start());
    }

    /**
     * Waits for {@value TIME_SYNCHRONIZATION_TIMEOUT} ms for the elected leader to respond.
     * <p>
     * If the elected leader has not responded yet, then its information is removed from
     * {@link #registeredIoTs}, and then
     * {@link #electLeaderAndSynchronizeClocks()} is rerun.
     */
    private void waitForResponseFromElectedLeader() {
        System.out.println("Waiting for Leader's response...");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (isTimeSynchronizationSuccessful()) {
                    setTimeSynchronizationSuccessful(false);
                } else {
                    System.out.println("Leader did not respond.");

                    System.out.println("Removing leader from Map of registered IoTs.");
                    removeHighestIoTFromRegisteredIoTsMap();

                    System.out.println("Rerunning Leader Election for Time Synchronization...");
                    electLeaderAndSynchronizeClocks();
                }
            }
        }, TIME_SYNCHRONIZATION_TIMEOUT);
    }

    /**
     * Removes the IoT with the highest {@link IoT#id} from {@link #registeredIoTs}.
     * <p>
     * This is done because the IoT with the highest id did not respond within
     * {@value TIME_SYNCHRONIZATION_TIMEOUT} ms with a {@link #leaderElected()} message.
     */
    private void removeHighestIoTFromRegisteredIoTsMap() {
        getRegisteredIoTs().keySet().stream()
                .max(Comparator.comparing(IoT::getId))
                .ifPresent(ioT -> getRegisteredIoTs().remove(ioT));
    }

    private boolean isTimeSynchronizationSuccessful() {
        return timeSynchronizationSuccessful;
    }

    private void setTimeSynchronizationSuccessful(final boolean timeSynchronizationSuccessful) {
        this.timeSynchronizationSuccessful = timeSynchronizationSuccessful;
    }
}
