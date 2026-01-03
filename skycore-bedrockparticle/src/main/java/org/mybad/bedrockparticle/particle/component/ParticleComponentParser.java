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
package org.mybad.bedrockparticle.particle.component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.Map;

/**
 * <p>Deserializes particle components from JSON.</p>
 * <p>The way components are parsed can be customized using the {@linkplain java.util.ServiceLoader Service Loader API},
 * otherwise a {@linkplain ParticleComponentParserImpl simple implementation} using case insensitive strings is used.</p>
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface ParticleComponentParser {

    /**
     * @return The parser instance to use
     */
    static ParticleComponentParser getInstance() {
        return ParticleComponentParserImpl.getInstance();
    }

    /**
     * Deserializes all components in the specified json.
     *
     * @param json The json to deserialize components from
     * @return All components deserialized
     * @throws JsonParseException If any errors occurs deserializing components
     */
    Map<String, ParticleComponent> deserialize(JsonObject json) throws JsonParseException;
}
