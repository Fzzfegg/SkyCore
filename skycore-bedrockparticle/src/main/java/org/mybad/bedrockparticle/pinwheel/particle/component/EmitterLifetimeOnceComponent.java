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
 * Component that spawns particles during the active time.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterLifetimeOnceComponent implements ParticleEmitterComponent {

    private final MolangExpression activeTime;

    public EmitterLifetimeOnceComponent(MolangExpression activeTime) {
        this.activeTime = activeTime;
    }

    public MolangExpression activeTime() {
        return activeTime;
    }

    public static final MolangExpression DEFAULT_ACTIVE_TIME = MolangExpression.of(10);

    public static EmitterLifetimeOnceComponent deserialize(JsonElement json) throws JsonParseException {
        return new EmitterLifetimeOnceComponent(JsonTupleParser.getExpression(json.getAsJsonObject(), "active_time", () -> EmitterLifetimeOnceComponent.DEFAULT_ACTIVE_TIME));
    }
}
