package com.smarthome.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smarthome.enums.DeviceType;
import com.smarthome.enums.EntrantType;
import com.smarthome.enums.SensorType;
import com.smarthome.gson.DeviceTypeDeserializer;
import com.smarthome.gson.EntrantTypeDeserializer;
import com.smarthome.gson.SensorTypeDeserializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigReader<T> {

    private final Class<T> clazz;
    private Gson gson;

    public ConfigReader(final Class<T> clazz) {
        this.clazz = clazz;
        initializeGson();
    }

    public T read(final String configFilePath) {
        JsonObject jsonObject = new JsonParser().parse(readFile(configFilePath)).getAsJsonObject();
        return getGson().fromJson(jsonObject, clazz);
    }

    /**
     * Reads the configuration JSON text from the given file path using the
     * Java 8 {@link Files#lines(Path)} API.
     *
     * @param configFilePath The file path to read the configuration from
     * @return The configuration JSON text which was read from the configuration file
     */
    private String readFile(final String configFilePath) {
        final StringBuilder builder = new StringBuilder();
        try {
            Files.lines(new File(configFilePath).toPath())
                    .forEach(builder::append);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    /**
     * Initializes the Gson object after setting Type Adapters for various Enums.
     */
    private void initializeGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DeviceType.class, new DeviceTypeDeserializer());
        gsonBuilder.registerTypeAdapter(EntrantType.class, new EntrantTypeDeserializer());
        gsonBuilder.registerTypeAdapter(SensorType.class, new SensorTypeDeserializer());
        setGson(gsonBuilder.create());
    }

    private Gson getGson() {
        return gson;
    }

    private void setGson(final Gson gson) {
        this.gson = gson;
    }
}
