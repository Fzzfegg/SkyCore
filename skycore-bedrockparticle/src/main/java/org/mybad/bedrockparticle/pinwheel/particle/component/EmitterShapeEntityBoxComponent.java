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
import org.mybad.bedrockparticle.pinwheel.particle.ParticleInstance;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleSourceObject;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Component that spawns particles in a box around an entity.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterShapeEntityBoxComponent implements ParticleEmitterShape {

    private final boolean surfaceOnly;
    @Nullable
    private final MolangExpression[] direction;
    private final boolean inwards;

    public EmitterShapeEntityBoxComponent(boolean surfaceOnly,
                                          @Nullable MolangExpression[] direction,
                                          boolean inwards) {
        this.surfaceOnly = surfaceOnly;
        this.direction = direction;
        this.inwards = inwards;
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

    public static EmitterShapeEntityBoxComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        boolean surfaceOnly = PinwheelGsonHelper.getAsBoolean(jsonObject, "surface_only", false);
        Either<Boolean, MolangExpression[]> dir = ParticleComponent.parseDirection(jsonObject, "direction");
        MolangExpression[] direction = dir.right().orElse(null);
        boolean inwards = dir.left().orElse(false);
        return new EmitterShapeEntityBoxComponent(surfaceOnly, direction, inwards);
    }

    @Override
    public void emitParticles(ParticleEmitterShape.Spawner spawner, int count) {
        ParticleSourceObject entity = spawner.getEntity();
        if (entity == null) {
            for (int i = 0; i < count; i++) {
                ParticleInstance particle = spawner.createParticle();
                MolangEnvironment environment = particle.getEnvironment();
                double dx = 0;
                double dy = 0;
                double dz = 0;
                if (this.direction != null) {
                    dx = environment.safeResolve(this.direction[0]);
                    dy = environment.safeResolve(this.direction[1]);
                    dz = environment.safeResolve(this.direction[2]);
                }
                spawner.setPositionVelocity(particle, 0, 0, 0, dx, dy, dz);
                spawner.spawnParticle(particle);
            }
            return;
        }

        Random random = spawner.getRandom();
        for (int i = 0; i < count; i++) {
            ParticleInstance particle = spawner.createParticle();
            MolangEnvironment environment = particle.getEnvironment();

            double radiusX = entity.getMaxX() / 2F;
            double radiusY = entity.getMaxY() / 2F;
            double radiusZ = entity.getMaxZ() / 2F;
            double offsetX = entity.getMinX() + radiusX;
            double offsetY = entity.getMinY() + radiusY;
            double offsetZ = entity.getMinZ() + radiusZ;
            double rx = this.surfaceOnly ? radiusX : radiusX * random.nextFloat();
            double ry = this.surfaceOnly ? radiusY : radiusY * random.nextFloat();
            double rz = this.surfaceOnly ? radiusZ : radiusZ * random.nextFloat();

            double x = (random.nextFloat() * 2 - 1) * rx;
            double y = (random.nextFloat() * 2 - 1) * ry;
            double z = (random.nextFloat() * 2 - 1) * rz;

            double dx;
            double dy;
            double dz;
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
