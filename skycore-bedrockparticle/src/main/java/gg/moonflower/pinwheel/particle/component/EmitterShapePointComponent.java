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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.ParticleInstance;

/**
 * Component that spawns particles in a disc.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterShapePointComponent implements ParticleEmitterShape {

    private final MolangExpression[] offset;
    private final MolangExpression[] direction;

    public EmitterShapePointComponent(MolangExpression[] offset, MolangExpression[] direction) {
        this.offset = offset;
        this.direction = direction;
    }

    public MolangExpression[] offset() {
        return offset;
    }

    public MolangExpression[] direction() {
        return direction;
    }

    public static EmitterShapePointComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        return new EmitterShapePointComponent(
                JsonTupleParser.getExpression(jsonObject, "offset", 3, () -> new MolangExpression[]{
                        MolangExpression.ZERO,
                        MolangExpression.ZERO,
                        MolangExpression.ZERO
                }),
                JsonTupleParser.getExpression(jsonObject, "direction", 3, () -> new MolangExpression[]{
                        MolangExpression.ZERO,
                        MolangExpression.ZERO,
                        MolangExpression.ZERO
                })
        );
    }

    @Override
    public void emitParticles(ParticleEmitterShape.Spawner spawner, int count) {
        for (int i = 0; i < count; i++) {
            ParticleInstance particle = spawner.createParticle();
            MolangEnvironment runtime = particle.getEnvironment();
            float x = runtime.safeResolve(this.offset[0]);
            float y = runtime.safeResolve(this.offset[1]);
            float z = runtime.safeResolve(this.offset[2]);
            float dx = runtime.safeResolve(this.direction[0]);
            float dy = runtime.safeResolve(this.direction[1]);
            float dz = runtime.safeResolve(this.direction[2]);
            spawner.setPositionVelocity(particle, x, y, z, dx, dy, dz);
            spawner.spawnParticle(particle);
        }
    }
}
