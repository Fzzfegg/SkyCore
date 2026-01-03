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
import com.google.gson.JsonSyntaxException;
import org.mybad.bedrockparticle.util.Either;
import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironment;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.json.JsonTupleParser;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleInstance;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Component that spawns particles in a disc.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterShapeDiscComponent implements ParticleEmitterShape {

    private final MolangExpression[] normal;
    private final MolangExpression[] offset;
    private final MolangExpression radius;
    private final boolean surfaceOnly;
    @Nullable
    private final MolangExpression[] direction;
    private final boolean inwards;

    public EmitterShapeDiscComponent(MolangExpression[] normal,
                                     MolangExpression[] offset,
                                     MolangExpression radius,
                                     boolean surfaceOnly,
                                     @Nullable MolangExpression[] direction,
                                     boolean inwards) {
        this.normal = normal;
        this.offset = offset;
        this.radius = radius;
        this.surfaceOnly = surfaceOnly;
        this.direction = direction;
        this.inwards = inwards;
    }

    public MolangExpression[] normal() {
        return normal;
    }

    public MolangExpression[] offset() {
        return offset;
    }

    public MolangExpression radius() {
        return radius;
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

    public static EmitterShapeDiscComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        MolangExpression[] normal;
        if (jsonObject.has("plane_normal")) {
            JsonElement planeJson = jsonObject.get("plane_normal");
            if (planeJson.isJsonPrimitive()) {
                String plane = PinwheelGsonHelper.convertToString(planeJson, "plane_normal");
                if ("x".equalsIgnoreCase(plane)) {
                    normal = new MolangExpression[]{
                            MolangExpression.of(1),
                            MolangExpression.ZERO,
                            MolangExpression.ZERO
                    };
                } else if ("y".equalsIgnoreCase(plane)) {
                    normal = new MolangExpression[]{
                            MolangExpression.ZERO,
                            MolangExpression.of(1),
                            MolangExpression.ZERO
                    };
                } else if ("z".equalsIgnoreCase(plane)) {
                    normal = new MolangExpression[]{
                            MolangExpression.ZERO,
                            MolangExpression.ZERO,
                            MolangExpression.of(1)
                    };
                } else {
                    throw new JsonSyntaxException("Expected plane_normal to be an axis(x, y, or z), was " + plane);
                }
            } else {
                normal = JsonTupleParser.getExpression(jsonObject, "plane_normal", 3, () -> new MolangExpression[]{
                        MolangExpression.ZERO,
                        MolangExpression.of(1),
                        MolangExpression.ZERO
                });
            }
        } else {
            normal = new MolangExpression[]{MolangExpression.ZERO, MolangExpression.of(1), MolangExpression.ZERO};
        }

        MolangExpression[] offset = JsonTupleParser.getExpression(jsonObject, "offset", 3, () -> new MolangExpression[]{
                MolangExpression.ZERO,
                MolangExpression.ZERO,
                MolangExpression.ZERO
        });
        MolangExpression radius = JsonTupleParser.getExpression(jsonObject, "radius", () -> MolangExpression.of(1));
        boolean surfaceOnly = PinwheelGsonHelper.getAsBoolean(jsonObject, "surface_only", false);
        Either<Boolean, MolangExpression[]> dir = ParticleComponent.parseDirection(jsonObject, "direction");
        MolangExpression[] direction = dir.right().orElse(null);
        boolean inwards = dir.left().orElse(false);
        return new EmitterShapeDiscComponent(normal, offset, radius, surfaceOnly, direction, inwards);
    }

    @Override
    public void emitParticles(ParticleEmitterShape.Spawner spawner, int count) {
        MolangEnvironment environment = spawner.getEnvironment();
        Random random = spawner.getRandom();
        float normalX = environment.safeResolve(this.normal[0]);
        float normalY = environment.safeResolve(this.normal[1]);
        float normalZ = environment.safeResolve(this.normal[2]);
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        normalX /= length;
        normalY /= length;
        normalZ /= length;

        float a = (float) Math.atan(normalX);
        float b = (float) Math.atan(normalY);
        float c = (float) Math.atan(normalZ);

        Quaternionf quaternion = new Quaternionf(0, 0, 0, 1).rotateZYX(c, b, a);
        Vector3f pos = new Vector3f();
        for (int i = 0; i < count; i++) {
            ParticleInstance particle = spawner.createParticle();
            environment = particle.getEnvironment();

            float offsetX = environment.safeResolve(this.offset[0]);
            float offsetY = environment.safeResolve(this.offset[1]);
            float offsetZ = environment.safeResolve(this.offset[2]);
            float radius = environment.safeResolve(this.radius);

            double r = this.surfaceOnly ? radius : radius * Math.sqrt(random.nextFloat());
            double theta = 2 * Math.PI * random.nextFloat();

            float x = (float) (r * Math.cos(theta));
            float y = (float) (r * Math.sin(theta));

            // ax + by + cz = 0

            float dx;
            float dy;
            float dz;
            if (this.direction != null) {
                float directionX = environment.safeResolve(this.direction[0]);
                float directionY = environment.safeResolve(this.direction[1]);
                float directionZ = environment.safeResolve(this.direction[2]);
                pos.set(directionX, directionY, directionZ);
                quaternion.transform(pos);
                dx = pos.x();
                dy = pos.y();
                dz = pos.z();
            } else {
                pos.set(x, 0, y);
                quaternion.transform(pos);

                dx = pos.x();
                dy = pos.y();
                dz = pos.z();
                if (this.inwards) {
                    dx = -dx;
                    dy = -dy;
                    dz = -dz;
                }
            }

            pos.set(x, 0, y);
            quaternion.transform(pos);

            spawner.setPositionVelocity(particle, offsetX + pos.x(), offsetY + pos.y(), offsetZ + pos.z(), dx, dy, dz);
            spawner.spawnParticle(particle);
        }
    }
}
