package com.smarthome.ioT;

import com.smarthome.enums.IoTType;
import com.smarthome.ioT.db.DbServer;
import com.smarthome.ioT.device.DeviceServer;
import com.smarthome.ioT.gateway.GatewayServer;
import com.smarthome.ioT.sensor.SensorServer;
import com.smarthome.model.Address;
import com.smarthome.model.IoT;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface IoTServer {

    Map<IoT, Long> offsetMap = new HashMap<>();

    IoT getIoT();

    /**
     * Gets the {@link Map} of registered IoTs.
     *
     * @return The {@link Map} of IoTs
     */
    Map<IoT, Address> getRegisteredIoTs();

    /**
     * Sets the {@link Map} of all IoTs which have been registered to the Gateway Server.
     *
     * @param registeredIoTs The Map to set within this server
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs) throws RemoteException;

    /**
     * Returns the System's current time after adjustment by adding an offset,
     * calculated using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>
     * for clock synchronization.
     *
     * @return The offset-adjusted System time
     */
    default long getSynchronizedTime() {
        return System.currentTimeMillis() + getSynchronizationOffset();
    }

    long getSynchronizationOffset();

    void setSynchronizationOffset(final long synchronizationOffset) throws RemoteException;

    /**
     * Checks if the current IoT has the highest UUID among all registered IoTs.
     *
     * @return {@code true} if current IoT has the highest UUID;
     * {@code false} otherwise
     */
    default boolean isLeader() {
        final List<UUID> uuidList = getRegisteredIoTs().keySet().stream()
                .map(IoT::getId)
                .collect(Collectors.toList());

        uuidList.sort(null);

        return getIoT().getId().equals(uuidList.get(uuidList.size() - 1));
    }

    default long getCurrentTime() throws RemoteException {
        return System.currentTimeMillis();
    }

    /**
     * Synchronizes the time of all IoTs in this distributed system using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    default void synchronizeTime() {
        buildOffsetMap();

        final long[] offsetTotal = {0};
        offsetMap.values().forEach(offset -> offsetTotal[0] += offset);

        sendSynchronizationOffsets(offsetTotal[0] / offsetMap.size());
    }

    /**
     * Builds a {@link Map} of all server times' offsets w.r.t. this server,
     * which will be later used to get the offset of the entire distributed system
     * and synchronize times using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     */
    default void buildOffsetMap() {
        offsetMap.clear();

        // Put self's offset as 0
        offsetMap.put(getIoT(), 0L);

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

                        offsetMap.put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
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

                        offsetMap.put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
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

                        offsetMap.put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
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

                        offsetMap.put(ioT, getOffset(requestTime, remoteCurrentTime, responseTime));
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    default void sendSynchronizationOffsets(final long offsetAverage) {
        // Send synchronization offset to self
        try {
            setSynchronizationOffset(
                    // TODO confirm the below formula, if this or reverse of it
                    offsetMap.get(getIoT()) - offsetAverage
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Send synchronization offset to Gateway Server
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.GATEWAY)
                .forEach(ioT -> {
                    final Address address = getRegisteredIoTs().get(ioT);
                    try {
                        GatewayServer.connect(address).setSynchronizationOffset(
                                // TODO confirm the below formula, if this or reverse of it
                                offsetMap.get(ioT) - offsetAverage
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
                                offsetMap.get(ioT) - offsetAverage
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
                                offsetMap.get(ioT) - offsetAverage
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
                                offsetMap.get(ioT) - offsetAverage
                        );
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    default long getOffset(final long requestTime, final long remoteCurrentTime,
            final long responseTime) {
        return requestTime - remoteCurrentTime + (responseTime - requestTime) / 2;
    }
}
