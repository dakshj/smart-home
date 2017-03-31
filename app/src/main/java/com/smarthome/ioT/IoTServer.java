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

    /**
     * Sets the synchronisationOffset of the sensor
     *
     * @param synchronizationOffset The value to which offset needs to be set
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    void setSynchronizationOffset(final long synchronizationOffset) throws RemoteException;

    /**
     * Sends the current system time of the sensor
     *
     * @return The current system time
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    default long getCurrentTime() throws RemoteException {
        return System.currentTimeMillis();
    }
}
