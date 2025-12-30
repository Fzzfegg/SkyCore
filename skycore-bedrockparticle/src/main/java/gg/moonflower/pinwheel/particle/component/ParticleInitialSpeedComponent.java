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
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;

/**
 * Component that specifies the initial speed of a particle.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleInitialSpeedComponent implements ParticleComponent {

    private final MolangExpression[] speed;

    public ParticleInitialSpeedComponent(MolangExpression[] speed) {
        this.speed = speed;
    }

    public MolangExpression[] speed() {
        return speed;
    }

    public static ParticleInitialSpeedComponent deserialize(JsonElement json) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            MolangExpression expression = JsonTupleParser.parseExpression(json, "speed");
            MolangExpression[] speed = new MolangExpression[]{expression, expression, expression};
            return new ParticleInitialSpeedComponent(speed);
        }

        if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.size() != 3) {
                throw new JsonSyntaxException("Expected speed to be a JsonArray of size 3, was " + jsonArray.size());
            }
            MolangExpression dx = JsonTupleParser.parseExpression(jsonArray.get(0), "speed[0]");
            MolangExpression dy = JsonTupleParser.parseExpression(jsonArray.get(1), "speed[1]");
            MolangExpression dz = JsonTupleParser.parseExpression(jsonArray.get(2), "speed[2]");
            MolangExpression[] speed = new MolangExpression[]{dx, dy, dz};
            return new ParticleInitialSpeedComponent(speed);
        }

        throw new JsonSyntaxException("Expected speed to be a JsonArray or float, was " + PinwheelGsonHelper.getType(json));
    }
}
