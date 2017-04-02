package com.smarthome.ioT;

import com.smarthome.model.Address;
import com.smarthome.model.IoT;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface IoTServer extends Remote {

    /**
     * Sets the {@link Map} of all IoTs which have been registered to the Gateway Server.
     *
     * @param registeredIoTs    The Map to set within this server
     * @param senderLogicalTime The logical time of the calling IoT server
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs, final long senderLogicalTime)
            throws RemoteException;

    /**
     * Sets the synchronization offset of this IoT server, calculated using the
     * <a href="https://en.wikipedia.org/wiki/Berkeley_algorithm">Berkeley algorithm</a>.
     *
     * @param synchronizationOffset The synchronization offset of the chronological clock
     * @param senderLogicalTime     The logical time of the calling IoT server
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setSynchronizationOffset(final long synchronizationOffset, final long senderLogicalTime)
            throws RemoteException;

    /**
     * Returns the current system time of this IoT server.
     *
     * @return The current system time
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    long getCurrentTime() throws RemoteException;

    void leaderElected() throws RemoteException;
}
