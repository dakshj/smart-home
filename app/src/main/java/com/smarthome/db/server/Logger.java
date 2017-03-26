package com.smarthome.db.server;

import com.smarthome.exception.LogFileCreationFailedException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class Logger {

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
     * Appends a single line to {@value #LOG_FILE_NAME}.
     *
     * @param line The line to be appended to {@value #LOG_FILE_NAME}
     */
    public void appendLine(final String line) {
        appendLines(Collections.singletonList(line));
    }

    /**
     * Appends multiple lines to {@value #LOG_FILE_NAME}.
     *
     * @param lines The {@link List} of lines to be appended to {@value #LOG_FILE_NAME}
     */
    public void appendLines(final List<String> lines) {
        try {
            Files.write(getLogFile().toPath(), lines, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to write lines to " + LOG_FILE_NAME + "!");
            e.printStackTrace();
        }
    }
}
