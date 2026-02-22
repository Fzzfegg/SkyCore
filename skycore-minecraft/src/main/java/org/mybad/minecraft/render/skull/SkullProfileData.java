package org.mybad.minecraft.render.skull;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.util.Collection;

final class SkullProfileData {
    private static final String PROPERTY_KEY = "model_profile";

    private final String mappingName;

    private SkullProfileData(String mappingName) {
        this.mappingName = mappingName;
    }

    String getMappingName() {
        return mappingName;
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
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return new SkullProfileData(value);
    }
}
