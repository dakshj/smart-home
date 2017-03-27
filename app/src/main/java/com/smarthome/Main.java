package com.smarthome;

import com.smarthome.entrant.server.EntrantServerImpl;
import com.smarthome.enums.ExecutionMode;
import com.smarthome.ioT.db.DbServerImpl;
import com.smarthome.ioT.device.DeviceServerImpl;
import com.smarthome.ioT.gateway.GatewayServerImpl;
import com.smarthome.ioT.sensor.SensorServerImpl;
import com.smarthome.model.config.Config;
import com.smarthome.model.config.DeviceConfig;
import com.smarthome.model.config.EntrantConfig;
import com.smarthome.model.config.GatewayConfig;
import com.smarthome.model.config.SensorConfig;
import com.smarthome.util.ConfigReader;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class Main {
    /**
     * @param args <p>
     *             args[0]:
     *             <p>
     *             Use "0" to start a Gateway Server
     *             <p>
     *             Use "1" to start a DB Server
     *             <p>
     *             Use "2" to start a Sensor Server
     *             <p>
     *             Use "3" to start a Device Server
     *             <p>
     *             Use "4" to start an Entrant Server
     *             <p>
     *             <p>
     *             args[1]:
     *             <p>
     *             Configuration JSON file path
     * @throws RemoteException Thrown when a Java RMI Exception occurs
     */
    public static void main(String[] args) throws RemoteException, UnknownHostException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No command-line arguments provided." +
                    " Please refer the JavaDoc to know more on these arguments.");
        }

        ExecutionMode executionMode;
        try {
            executionMode = ExecutionMode.from(Integer.parseInt(args[0]));
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Execution Mode is invalid.");
        }

        assert executionMode != null;

        String configFilePath = args[1];

        switch (executionMode) {
            case GATEWAY: {
                ConfigReader<GatewayConfig> reader = new ConfigReader<>(GatewayConfig.class);
                final GatewayConfig gatewayConfig = reader.read(configFilePath);
                new GatewayServerImpl(gatewayConfig);
            }
            break;

            case DB: {
                ConfigReader<Config> reader = new ConfigReader<>(Config.class);
                final Config config = reader.read(configFilePath);
                new DbServerImpl(config);
            }
            break;

            case SENSOR: {
                ConfigReader<SensorConfig> reader = new ConfigReader<>(SensorConfig.class);
                final SensorConfig sensorConfig = reader.read(configFilePath);
                new SensorServerImpl(sensorConfig);
            }
            break;

            case DEVICE: {
                ConfigReader<DeviceConfig> reader = new ConfigReader<>(DeviceConfig.class);
                final DeviceConfig deviceConfig = reader.read(configFilePath);
                new DeviceServerImpl(deviceConfig);
            }
            break;

            case ENTRANT: {
                ConfigReader<EntrantConfig> reader = new ConfigReader<>(EntrantConfig.class);
                final EntrantConfig entrantConfig = reader.read(configFilePath);
                new EntrantServerImpl(entrantConfig);
            }
            break;
        }
    }
}
