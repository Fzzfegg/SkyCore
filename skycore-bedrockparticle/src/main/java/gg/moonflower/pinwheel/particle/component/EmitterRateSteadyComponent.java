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
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;

/**
 * Component that summons particles at a steady rate until too many particles are spawned.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterRateSteadyComponent implements ParticleEmitterComponent {

    private final MolangExpression spawnRate;
    private final MolangExpression maxParticles;

    public EmitterRateSteadyComponent(MolangExpression spawnRate, MolangExpression maxParticles) {
        this.spawnRate = spawnRate;
        this.maxParticles = maxParticles;
    }

    public MolangExpression spawnRate() {
        return spawnRate;
    }

    public MolangExpression maxParticles() {
        return maxParticles;
    }

    public static final MolangExpression DEFAULT_SPAWN_RATE = MolangExpression.of(1);
    public static final MolangExpression DEFAULT_MAX_PARTICLES = MolangExpression.of(50);

    public static EmitterRateSteadyComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new EmitterRateSteadyComponent(
                JsonTupleParser.getExpression(object, "spawn_rate", () -> EmitterRateSteadyComponent.DEFAULT_SPAWN_RATE),
                JsonTupleParser.getExpression(object, "max_particles", () -> EmitterRateSteadyComponent.DEFAULT_MAX_PARTICLES));
    }
}
