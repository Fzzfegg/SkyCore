/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'gg.moonflower.pinwheel.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package gg.moonflower.pinwheel.particle.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;

/**
 * Component that specifies what blocks particles will not expire in.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleExpireNotInBlocksComponent implements ParticleComponent {

    private final String[] blocks;

    public ParticleExpireNotInBlocksComponent(String[] blocks) {
        this.blocks = blocks;
    }

    public String[] blocks() {
        return blocks;
    }

    public static ParticleExpireNotInBlocksComponent deserialize(JsonElement json) throws JsonParseException {
        JsonArray jsonObject = json.getAsJsonArray();
        String[] blocks = new String[jsonObject.size()];
        try {
            for (int i = 0; i < jsonObject.size(); i++) {
                blocks[i] = PinwheelGsonHelper.convertToString(jsonObject.get(i), "minecraft:particle_expire_if_not_in_blocks[" + i + "]");
            }
        } catch (Exception e) {
            throw new JsonSyntaxException(e);
        }
        return new ParticleExpireNotInBlocksComponent(blocks);
    }
}
