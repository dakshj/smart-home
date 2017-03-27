package com.smarthome.ioT;

import com.smarthome.enums.IoTType;
import com.smarthome.ioT.db.DbServer;
import com.smarthome.model.Address;
import com.smarthome.model.IoT;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface IoTServer {

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

    default void synchronizeTime() {
        // TODO
        // Request time from DB
        getRegisteredIoTs().keySet().stream()
                .filter(ioT -> !getIoT().equals(ioT))
                .filter(ioT -> ioT.getIoTType() == IoTType.DB)
                .map(ioT -> getRegisteredIoTs().get(ioT))
                .forEach(address -> {
                    try {
                        DbServer.connect(address).getCurrentTime();
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                });
    }
}
