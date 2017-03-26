package com.smarthome.entrant.server;

import com.smarthome.gateway.server.GatewayServer;
import com.smarthome.model.Address;
import com.smarthome.model.Entrant;
import com.smarthome.model.IoT;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class EntrantServerImpl extends UnicastRemoteObject implements EntrantServer {

    private final Entrant entrant;
    private long synchronizationOffset;
    private Set<IoT> ioTs;

    public EntrantServerImpl(final Entrant entrant, final Address selfAddress,
            final Address gatewayAddress) throws RemoteException {
        this.entrant = entrant;
        startServer(selfAddress.getPortNo());

        try {
            setIoTs(GatewayServer.connect(gatewayAddress).getIoTs());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * Starts the Entrant Server on the provided port number.
     * Uses {@value #NAME} as the name to associate with the remote reference.
     *
     * @param portNo The port number to start the Entrant Server on
     * @throws RemoteException Thrown when a Java RMI exception occurs
     */
    private void startServer(final int portNo) throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(portNo);
        registry.rebind(NAME, this);
    }

    private Entrant getEntrant() {
        return entrant;
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

    private Set<IoT> getIoTs() {
        return ioTs;
    }

    private void setIoTs(final Set<IoT> ioTs) {
        this.ioTs = ioTs;
    }
}
