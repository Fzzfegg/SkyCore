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
 * Component that controls when an emitter can produce particles and if it should be removed.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterLifetimeExpressionComponent implements ParticleEmitterComponent {

    private final MolangExpression activation;
    private final MolangExpression expiration;

    public EmitterLifetimeExpressionComponent(MolangExpression activation, MolangExpression expiration) {
        this.activation = activation;
        this.expiration = expiration;
    }

    public MolangExpression activation() {
        return activation;
    }

    public MolangExpression expiration() {
        return expiration;
    }

    public static final MolangExpression DEFAULT_ACTIVATION = MolangExpression.of(1);
    public static final MolangExpression DEFAULT_EXPIRATION = MolangExpression.ZERO;

    public static EmitterLifetimeExpressionComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new EmitterLifetimeExpressionComponent(
                ParticleJsonTupleParser.getExpression(object, "activation_expression", () -> EmitterLifetimeExpressionComponent.DEFAULT_ACTIVATION),
                ParticleJsonTupleParser.getExpression(object, "expiration_expression", () -> EmitterLifetimeExpressionComponent.DEFAULT_EXPIRATION));
    }
}
