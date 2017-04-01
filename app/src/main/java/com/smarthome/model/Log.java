package com.smarthome.model;

import com.smarthome.enums.DeviceType;
import com.smarthome.enums.IoTType;
import com.smarthome.enums.LogType;
import com.smarthome.enums.SensorType;

import java.io.Serializable;
import java.util.UUID;

public class Log implements Serializable {

    private final LogType logType;
    private final UUID id;
    private final IoTType ioTType;
    private final SensorType sensorType;
    private final DeviceType deviceType;
    private final long chronologicalTime;
    private final long logicalTime;
    private final String message;

    public Log(final LogType logType, final UUID id, final IoTType ioTType, final SensorType sensorType,
            final DeviceType deviceType, final long chronologicalTime, final long logicalTime,
            final String message) {
        this.logType = logType;
        this.id = id;
        this.ioTType = ioTType;
        this.sensorType = sensorType;
        this.deviceType = deviceType;
        this.chronologicalTime = chronologicalTime;
        this.logicalTime = logicalTime;
        this.message = message;
    }

    public LogType getLogType() {
        return logType;
    }

    public UUID getId() {
        return id;
    }

    public IoTType getIoTType() {
        return ioTType;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public long getChronologicalTime() {
        return chronologicalTime;
    }

    public long getLogicalTime() {
        return logicalTime;
    }

    public String getMessage() {
        return message;
    }
}
