package com.smarthome.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.smarthome.enums.EntrantType;

import java.lang.reflect.Type;

public class EntrantTypeDeserializer implements JsonDeserializer {

    @Override
    public EntrantType deserialize(final JsonElement json, final Type typeOfT,
            final JsonDeserializationContext context) throws JsonParseException {
        int typeInt = json.getAsInt();
        return EntrantType.from(typeInt);
    }
}
