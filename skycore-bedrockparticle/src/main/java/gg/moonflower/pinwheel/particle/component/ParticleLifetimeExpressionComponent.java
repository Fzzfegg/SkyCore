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
 * Component that controls when a particle should be removed and how long it can live for.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleLifetimeExpressionComponent implements ParticleComponent {

    private final MolangExpression expirationExpression;
    private final MolangExpression maxLifetime;

    public ParticleLifetimeExpressionComponent(MolangExpression expirationExpression, MolangExpression maxLifetime) {
        this.expirationExpression = expirationExpression;
        this.maxLifetime = maxLifetime;
    }

    public MolangExpression expirationExpression() {
        return expirationExpression;
    }

    public MolangExpression maxLifetime() {
        return maxLifetime;
    }

    public static final MolangExpression DEFAULT_EXPIRATION = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_MAX_LIFETIME = MolangExpression.of(1);

    public static ParticleLifetimeExpressionComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new ParticleLifetimeExpressionComponent(
                JsonTupleParser.getExpression(object, "expiration_expression", () -> ParticleLifetimeExpressionComponent.DEFAULT_EXPIRATION),
                JsonTupleParser.getExpression(object, "max_lifetime", () -> ParticleLifetimeExpressionComponent.DEFAULT_MAX_LIFETIME));
    }
}
