package org.mybad.minecraft.render.skull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.util.Collection;

final class SkullProfileData {
    private static final String PROPERTY_KEY = "model_profile";
    private static final JsonParser PARSER = new JsonParser();

    private final String mappingName;
    private final String clip;
    private final Float scale;
    private final Boolean loop;
    private final Boolean globalRender;

    private SkullProfileData(String mappingName, String clip, Float scale, Boolean loop, Boolean globalRender) {
        this.mappingName = mappingName;
        this.clip = clip;
        this.scale = scale;
        this.loop = loop;
        this.globalRender = globalRender;
    }

    String getMappingName() {
        return mappingName;
    }

    String getClip() {
        return clip;
    }

    Float getScale() {
        return scale;
    }

    Boolean getLoop() {
        return loop;
    }

    Boolean getGlobalRender() {
        return globalRender;
    }

    static SkullProfileData from(GameProfile profile) {
        if (profile == null) {
            return null;
        }
        PropertyMap properties = profile.getProperties();
        if (properties == null) {
            return null;
        }
        Collection<Property> values = properties.get(PROPERTY_KEY);
        if (values == null || values.isEmpty()) {
            return null;
        }
        Property property = values.iterator().next();
        if (property == null) {
            return null;
        }
        return parseValue(property.getValue());
    }

    private static SkullProfileData parseValue(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            try {
                JsonElement element = PARSER.parse(value);
                if (!element.isJsonObject()) {
                    return null;
                }
                JsonObject obj = element.getAsJsonObject();
                String mapping = getString(obj, "mapping");
                if (mapping == null || mapping.isEmpty()) {
                    return null;
                }
                String clip = getString(obj, "clip");
                Float scale = getFloat(obj, "scale");
                Boolean loop = getBoolean(obj, "loop");
                Boolean global = getBoolean(obj, "global_render");
                if (global == null) {
                    global = getBoolean(obj, "globalRender");
                }
                return new SkullProfileData(mapping, clip, scale, loop, global);
            } catch (JsonSyntaxException ignored) {
                return simpleValue(value);
            }
        }
        return simpleValue(value);
    }

    private static SkullProfileData simpleValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return new SkullProfileData(value, null, null, null, null);
    }

    private static String getString(JsonObject obj, String member) {
        if (obj == null || !obj.has(member)) {
            return null;
        }
        JsonElement element = obj.get(member);
        if (!element.isJsonPrimitive()) {
            return null;
        }
        String text = element.getAsString();
        return text != null ? text.trim() : null;
    }

    private static Float getFloat(JsonObject obj, String member) {
        if (obj == null || !obj.has(member)) {
            return null;
        }
        try {
            return obj.get(member).getAsFloat();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean getBoolean(JsonObject obj, String member) {
        if (obj == null || !obj.has(member)) {
            return null;
        }
        try {
            return obj.get(member).getAsBoolean();
        } catch (Exception ignored) {
            return null;
        }
    }
}
