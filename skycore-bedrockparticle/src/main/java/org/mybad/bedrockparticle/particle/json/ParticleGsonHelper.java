/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'org.mybad.bedrockparticle.particle.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.particle.json;

import com.google.gson.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ParticleGsonHelper {

    public static String convertToString(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a string, was " + getType(element));
        }
    }

    public static String getAsString(JsonObject json, String name) throws JsonSyntaxException {
        if (json.has(name)) {
            return convertToString(json.get(name), name);
        } else {
            throw new JsonSyntaxException("Missing " + name + ", expected to find a string");
        }
    }

    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static String getAsString(JsonObject json, String name, @Nullable String defaultValue) throws JsonSyntaxException {
        return json.has(name) ? convertToString(json.get(name), name) : defaultValue;
    }

    public static boolean convertToBoolean(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonPrimitive()) {
            return element.getAsBoolean();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a Boolean, was " + getType(element));
        }
    }

    public static boolean getAsBoolean(JsonObject json, String name, boolean defaultValue) throws JsonSyntaxException {
        return json.has(name) ? convertToBoolean(json.get(name), name) : defaultValue;
    }

    public static float convertToFloat(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsFloat();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a Float, was " + getType(element));
        }
    }

    public static float getAsFloat(JsonObject json, String name, float defaultValue) throws JsonSyntaxException {
        return json.has(name) ? convertToFloat(json.get(name), name) : defaultValue;
    }

    public static int convertToInt(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a Int, was " + getType(element));
        }
    }

    public static int getAsInt(JsonObject json, String name, int defaultValue) throws JsonSyntaxException {
        return json.has(name) ? convertToInt(json.get(name), name) : defaultValue;
    }

    public static short convertToShort(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsShort();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a Short, was " + getType(element));
        }
    }

    public static JsonObject convertToJsonObject(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + getType(element));
        }
    }

    public static JsonObject getAsJsonObject(JsonObject json, String name) throws JsonSyntaxException {
        if (json.has(name)) {
            return convertToJsonObject(json.get(name), name);
        } else {
            throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonObject");
        }
    }

    public static JsonArray convertToJsonArray(JsonElement element, String name) throws JsonSyntaxException {
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        } else {
            throw new JsonSyntaxException("Expected " + name + " to be a JsonArray, was " + getType(element));
        }
    }

    public static JsonArray getAsJsonArray(JsonObject json, String name) throws JsonSyntaxException {
        if (json.has(name)) {
            return convertToJsonArray(json.get(name), name);
        } else {
            throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray");
        }
    }

    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static JsonArray getAsJsonArray(JsonObject json, String name, @Nullable JsonArray defaultValue) throws JsonSyntaxException {
        return json.has(name) ? convertToJsonArray(json.get(name), name) : defaultValue;
    }

    public static String getType(@Nullable JsonElement element) {
        String string = abbreviateMiddle(String.valueOf(element), "...", 10);
        if (element == null) {
            return "null (missing)";
        } else if (element.isJsonNull()) {
            return "null (json)";
        } else if (element.isJsonArray()) {
            return "an array (" + string + ")";
        } else if (element.isJsonObject()) {
            return "an object (" + string + ")";
        } else {
            if (element.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();
                if (jsonPrimitive.isNumber()) {
                    return "a number (" + string + ")";
                }

                if (jsonPrimitive.isBoolean()) {
                    return "a boolean (" + string + ")";
                }
            }

            return string;
        }
    }

    private static String abbreviateMiddle(String str, String middle, int maxLength) {
        if (str == null || middle == null) {
            return str;
        }
        if (maxLength < middle.length() + 2) {
            return str;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        int target = maxLength - middle.length();
        int start = target / 2 + target % 2;
        int end = target / 2;
        return str.substring(0, start) + middle + str.substring(str.length() - end);
    }
}
