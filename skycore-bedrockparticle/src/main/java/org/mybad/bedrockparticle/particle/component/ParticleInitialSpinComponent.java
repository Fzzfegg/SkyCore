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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.json.ParticleJsonTupleParser;

/**
 * Component that specifies the initial rotation and rotation rate of a particle.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleInitialSpinComponent implements ParticleComponent {

    private final MolangExpression rotation;
    private final MolangExpression rotationRate;

    public ParticleInitialSpinComponent(MolangExpression rotation, MolangExpression rotationRate) {
        this.rotation = rotation;
        this.rotationRate = rotationRate;
    }

    public MolangExpression rotation() {
        return rotation;
    }

    public MolangExpression rotationRate() {
        return rotationRate;
    }

    public static ParticleInitialSpinComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        MolangExpression rotation = ParticleJsonTupleParser.getExpression(object, "rotation", () -> MolangExpression.ZERO);
        MolangExpression rotationRate = ParticleJsonTupleParser.getExpression(object, "rotation_rate", () -> MolangExpression.ZERO);
        return new ParticleInitialSpinComponent(rotation, rotationRate);
    }
}
