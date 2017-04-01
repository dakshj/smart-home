package com.smarthome.ioT.db;

import com.smarthome.exception.LogFileCreationFailedException;
import com.smarthome.model.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

class Logger {

    private static final String LOG_FILE_NAME = "Smart Home Logs.txt";

    private final File logFile;

    /**
     * Initializes (and creates the file on disk, if necessary) a {@link File} object pointing to
     * {@value #LOG_FILE_NAME}.
     */
    Logger() {
        final URL resource = ClassLoader.getSystemClassLoader().getResource(".");
        if (resource == null) {
            throw new LogFileCreationFailedException("{failed to fetch folder path}");
        }

        final File jarDir = new File(resource.getPath());
        logFile = new File(jarDir, LOG_FILE_NAME);

        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    throw new LogFileCreationFailedException(logFile.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getLogFile() {
        return logFile;
    }

    /**
     * Logs the {@link Log} model as a single line to {@value #LOG_FILE_NAME}.
     *
     * @param log The object to log
     */
    void log(final Log log) {
        appendLine("" +
                (log.getLogType() != null ? log.getLogType().ordinal() : null) +
                ',' +
                log.getId() +
                ',' +
                (log.getIoTType() != null ? log.getIoTType().ordinal() : null) +
                ',' +
                (log.getSensorType() != null ? log.getSensorType().ordinal() : null) +
                ',' +
                (log.getDeviceType() != null ? log.getDeviceType().ordinal() : null) +
                ',' +
                log.getChronologicalTime() +
                ',' +
                log.getLogicalTime() +
                ',' +
                log.getMessage()
        );
    }

    /**
     * Appends a single line to {@value #LOG_FILE_NAME}.
     *
     * @param line The line to be appended to {@value #LOG_FILE_NAME}
     */
    private void appendLine(final String line) {
        try {
            Files.write(getLogFile().toPath(), Collections.singletonList(line),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to write lines to " + LOG_FILE_NAME + "!");
            e.printStackTrace();
        }
    }
}
