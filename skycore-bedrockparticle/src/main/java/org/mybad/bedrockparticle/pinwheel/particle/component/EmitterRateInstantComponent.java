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
import com.google.gson.JsonParseException;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.json.JsonTupleParser;

/**
 * Component that summons particles once.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterRateInstantComponent implements ParticleEmitterComponent {

    private final MolangExpression particleCount;

    public EmitterRateInstantComponent(MolangExpression particleCount) {
        this.particleCount = particleCount;
    }

    public MolangExpression particleCount() {
        return particleCount;
    }

    public static final MolangExpression DEFAULT_PARTICLE_COUNT = MolangExpression.of(10);

    public static EmitterRateInstantComponent deserialize(JsonElement json) throws JsonParseException {
        return new EmitterRateInstantComponent(JsonTupleParser.getExpression(json.getAsJsonObject(), "num_particles", () -> EmitterRateInstantComponent.DEFAULT_PARTICLE_COUNT));
    }

    @Override
    public boolean canLoop() {
        return true;
    }
}
