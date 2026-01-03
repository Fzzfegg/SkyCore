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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;

/**
 * Component that kills all particles that pass over a plane. Uses the standard <code>ax + by + cz + d = 0</code> form.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleKillPlaneComponent implements ParticleComponent {

    private final float a;
    private final float b;
    private final float c;
    private final float d;

    public ParticleKillPlaneComponent(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public float a() {
        return a;
    }

    public float b() {
        return b;
    }

    public float c() {
        return c;
    }

    public float d() {
        return d;
    }

    public ParticleKillPlaneComponent(float[] coefficients) {
        this(coefficients[0], coefficients[1], coefficients[2], coefficients[3]);
    }

    public static ParticleKillPlaneComponent deserialize(JsonElement json) throws JsonParseException {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            throw new JsonSyntaxException("Molang expressions are not supported");
        }
        if (!json.isJsonArray()) {
            throw new JsonSyntaxException("Expected minecraft:particle_kill_plane to be a JsonArray, was " + PinwheelGsonHelper.getType(json));
        }

        JsonArray vectorJson = json.getAsJsonArray();
        if (vectorJson.size() != 4) {
            throw new JsonParseException("Expected 4 minecraft:particle_kill_plane values, was " + vectorJson.size());
        }

        float[] coefficients = new float[4];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = PinwheelGsonHelper.convertToFloat(vectorJson.get(i), "minecraft:particle_kill_plane[" + i + "]");
        }
        return new ParticleKillPlaneComponent(coefficients);
    }

    private double solve(double x, double y, double z) {
        return this.a * x + this.b * y + this.c * z + this.d;
    }

    /**
     * Solves the plane equation for the old and current position.
     *
     * @param oldX The old x to solve for
     * @param oldY The old y to solve for
     * @param oldZ The old z to solve for
     * @param x    The new x to solve for
     * @param y    The new y to solve for
     * @param z    The new z to solve for
     * @return Whether the change from the old position to the new position crossed the plane
     */
    public boolean solve(double oldX, double oldY, double oldZ, double x, double y, double z) {
        double old = this.solve(oldX, oldY, oldZ);
        double current = this.solve(x, y, z);
        return Math.signum(old) != Math.signum(current);
    }
}
