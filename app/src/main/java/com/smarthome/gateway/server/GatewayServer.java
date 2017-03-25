package com.smarthome.gateway.server;

import com.smarthome.device.server.DeviceServer;
import com.smarthome.enums.DeviceType;
import com.smarthome.enums.SensorType;
import com.smarthome.model.Address;
import com.smarthome.model.Device;
import com.smarthome.model.Sensor;
import com.smarthome.sensor.server.SensorServer;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public interface GatewayServer extends Remote {

    String NAME = "Gateway Server";

    /**
     * Establishes a connection with a {@link GatewayServer}.
     *
     * @param address The address of the {@link GatewayServer}
     * @return An instance of the connected {@link GatewayServer}, connected remotely via Java RMI
     * @throws RemoteException   Thrown when a Java RMI exception occurs
     * @throws NotBoundException Thrown when the remote binding does not exist in the {@link Registry}
     */
    static GatewayServer connect(final Address address)
            throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNo());
        return (GatewayServer) registry.lookup(NAME);
    }

    /**
     * Registers a sensor with the gateway.
     * <p>
     * Stores the UUID and Address of the sensor in a {@link java.util.Map}.
     *
     * @param sensorType The type of the sensor
     * @param address    The address of the {@link SensorServer}
     * @return The sensor model object, which needs to be stored at the {@link SensorServer}
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    Sensor register(final SensorType sensorType, final Address address) throws RemoteException;

    /**
     * Registers a device with the gateway.
     * <p>
     * Stores the UUID and Address of the device in a {@link java.util.Map}.
     *
     * @param deviceType The type of the device
     * @param address    The address of the {@link DeviceServer}
     * @return The device model object, which needs to be stored at the {@link DeviceType}
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    Device register(final DeviceType deviceType, final Address address) throws RemoteException;

    /**
     * Queries the current state of either a sensor or a device.
     *
     * @param id The identifier of that sensor or device
     */
    void queryState(final UUID id);

    /**
     * Reports the current state of the sensor.
     *
     * @param sensor The sensor model object, containing the current state of the sensor
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    void reportState(final Sensor sensor) throws RemoteException;

    /**
     * Reports the current state of the device.
     *
     * @param device The device model object, containing the current state of the device
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    void reportState(final Device device) throws RemoteException;

    /**
     * Changes the state of the device.
     *
     * @param id    The identifier of the device whose state needs to be changed
     * @param state The new state of the device
     */
    void changeDeviceState(final UUID id, final boolean state);
}
