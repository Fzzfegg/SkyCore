/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'org.mybad.bedrockparticle.pinwheel.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.pinwheel.particle.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.json.JsonTupleParser;
import org.jetbrains.annotations.Nullable;

/**
 * Component that specifies how a particle moves over time directly.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleMotionParametricComponent implements ParticleComponent {

    @Nullable
    private final MolangExpression[] relativePosition;
    @Nullable
    private final MolangExpression[] direction;
    private final MolangExpression rotation;

    public ParticleMotionParametricComponent(@Nullable MolangExpression[] relativePosition,
                                             @Nullable MolangExpression[] direction,
                                             MolangExpression rotation) {
        this.relativePosition = relativePosition;
        this.direction = direction;
        this.rotation = rotation;
    }

    @Nullable
    public MolangExpression[] relativePosition() {
        return relativePosition;
    }

    @Nullable
    public MolangExpression[] direction() {
        return direction;
    }

    public MolangExpression rotation() {
        return rotation;
    }

    public static ParticleMotionParametricComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        MolangExpression[] relativePosition = JsonTupleParser.getExpression(object, "relative_position", 3, () -> null);
        MolangExpression[] direction = JsonTupleParser.getExpression(object, "direction", 3, () -> null);
        MolangExpression rotation = JsonTupleParser.getExpression(object, "rotation", () -> MolangExpression.ZERO);
        return new ParticleMotionParametricComponent(relativePosition, direction, rotation);
    }
}
