package com.smarthome.ioT;

import com.smarthome.model.Address;
import com.smarthome.model.IoT;

import java.rmi.RemoteException;
import java.util.Map;

public interface IoTServer {

    /**
     * Sets the {@link Map} of all IoTs which have been registered to the Gateway Server.
     *
     * @param registeredIoTs    The Map to set within this server
     * @param senderLogicalTime The logical time of the calling IoT server
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setRegisteredIoTs(final Map<IoT, Address> registeredIoTs, final long senderLogicalTime)
            throws RemoteException;

    void setSynchronizationOffset(final long synchronizationOffset) throws RemoteException;

    /**
     * Returns the current system time of this IoT server.
     *
     * @return The current system time
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    default long getCurrentTime() throws RemoteException {
        return System.currentTimeMillis();
    }
}
