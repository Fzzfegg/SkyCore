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
import org.mybad.bedrockparticle.util.Either;
import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironment;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.json.JsonTupleParser;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleInstance;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Component that spawns particles in a box.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterShapeBoxComponent implements ParticleEmitterShape {

    private final MolangExpression[] offset;
    private final MolangExpression[] halfDimensions;
    private final boolean surfaceOnly;
    @Nullable
    private final MolangExpression[] direction;
    private final boolean inwards;

    public EmitterShapeBoxComponent(MolangExpression[] offset,
                                    MolangExpression[] halfDimensions,
                                    boolean surfaceOnly,
                                    @Nullable MolangExpression[] direction,
                                    boolean inwards) {
        this.offset = offset;
        this.halfDimensions = halfDimensions;
        this.surfaceOnly = surfaceOnly;
        this.direction = direction;
        this.inwards = inwards;
    }

    public MolangExpression[] offset() {
        return offset;
    }

    public MolangExpression[] halfDimensions() {
        return halfDimensions;
    }

    public boolean surfaceOnly() {
        return surfaceOnly;
    }

    @Nullable
    public MolangExpression[] direction() {
        return direction;
    }

    public boolean inwards() {
        return inwards;
    }

    public static EmitterShapeBoxComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        MolangExpression[] offset = JsonTupleParser.getExpression(jsonObject, "offset", 3, () -> new MolangExpression[]{
                MolangExpression.ZERO,
                MolangExpression.ZERO,
                MolangExpression.ZERO
        });
        MolangExpression[] halfDimensions = JsonTupleParser.getExpression(jsonObject, "half_dimensions", 3, null);
        boolean surfaceOnly = PinwheelGsonHelper.getAsBoolean(jsonObject, "surface_only", false);
        Either<Boolean, MolangExpression[]> dir = ParticleComponent.parseDirection(jsonObject, "direction");
        MolangExpression[] direction = dir.right().orElse(null);
        boolean inwards = dir.left().orElse(false);
        return new EmitterShapeBoxComponent(offset, halfDimensions, surfaceOnly, direction, inwards);
    }

    @Override
    public void emitParticles(ParticleEmitterShape.Spawner spawner, int count) {
        Random random = spawner.getRandom();
        for (int i = 0; i < count; i++) {
            ParticleInstance particle = spawner.createParticle();
            MolangEnvironment environment = particle.getEnvironment();

            float offsetX = environment.safeResolve(this.offset[0]);
            float offsetY = environment.safeResolve(this.offset[1]);
            float offsetZ = environment.safeResolve(this.offset[2]);
            float radiusX = environment.safeResolve(this.halfDimensions[0]);
            float radiusY = environment.safeResolve(this.halfDimensions[1]);
            float radiusZ = environment.safeResolve(this.halfDimensions[2]);
            float rx = this.surfaceOnly ? radiusX : radiusX * random.nextFloat();
            float ry = this.surfaceOnly ? radiusY : radiusY * random.nextFloat();
            float rz = this.surfaceOnly ? radiusZ : radiusZ * random.nextFloat();

            float x = (random.nextFloat() * 2 - 1) * rx;
            float y = (random.nextFloat() * 2 - 1) * ry;
            float z = (random.nextFloat() * 2 - 1) * rz;

            float dx;
            float dy;
            float dz;
            if (this.direction != null) {
                dx = environment.safeResolve(this.direction[0]);
                dy = environment.safeResolve(this.direction[1]);
                dz = environment.safeResolve(this.direction[2]);
            } else {
                dx = x;
                dy = y;
                dz = z;
                if (this.inwards) {
                    dx = -dx;
                    dy = -dy;
                    dz = -dz;
                }
            }

            spawner.setPositionVelocity(particle, offsetX + x, offsetY + y, offsetZ + z, dx, dy, dz);
            spawner.spawnParticle(particle);
        }
    }
}
