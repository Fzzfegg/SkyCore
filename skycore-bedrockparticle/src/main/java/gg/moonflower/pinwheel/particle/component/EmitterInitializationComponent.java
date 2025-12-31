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
import org.jetbrains.annotations.Nullable;

/**
 * Component that initializes emitters.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterInitializationComponent implements ParticleEmitterComponent {

    @Nullable
    private final MolangExpression creationExpression;
    @Nullable
    private final MolangExpression tickExpression;
    @Nullable
    private final MolangExpression renderExpression;

    public EmitterInitializationComponent(@Nullable MolangExpression creationExpression,
                                          @Nullable MolangExpression tickExpression,
                                          @Nullable MolangExpression renderExpression) {
        this.creationExpression = creationExpression;
        this.tickExpression = tickExpression;
        this.renderExpression = renderExpression;
    }

    @Nullable
    public MolangExpression creationExpression() {
        return creationExpression;
    }

    @Nullable
    public MolangExpression tickExpression() {
        return tickExpression;
    }

    @Nullable
    public MolangExpression renderExpression() {
        return renderExpression;
    }

    public static EmitterInitializationComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new EmitterInitializationComponent(
                JsonTupleParser.getExpression(object, "creation_expression", () -> null),
                JsonTupleParser.getExpression(object, "per_update_expression", () -> null),
                JsonTupleParser.getExpression(object, "per_render_expression", () -> null));
    }

    @Override
    public boolean canLoop() {
        return true;
    }
}
