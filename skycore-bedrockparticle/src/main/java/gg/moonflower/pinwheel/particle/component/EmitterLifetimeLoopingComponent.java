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
 * Component that spawns particles during the active time.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterLifetimeLoopingComponent implements ParticleEmitterComponent {

    private final MolangExpression activeTime;
    private final MolangExpression sleepTime;

    public EmitterLifetimeLoopingComponent(MolangExpression activeTime, MolangExpression sleepTime) {
        this.activeTime = activeTime;
        this.sleepTime = sleepTime;
    }

    public MolangExpression activeTime() {
        return activeTime;
    }

    public MolangExpression sleepTime() {
        return sleepTime;
    }

    public static final MolangExpression DEFAULT_ACTIVE_TIME = MolangExpression.of(10);
    public static final MolangExpression DEFAULT_SLEEP_TIME = MolangExpression.ZERO;

    public static EmitterLifetimeLoopingComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new EmitterLifetimeLoopingComponent(
                JsonTupleParser.getExpression(object, "active_time", () -> EmitterLifetimeLoopingComponent.DEFAULT_ACTIVE_TIME),
                JsonTupleParser.getExpression(object, "sleep_time", () -> EmitterLifetimeLoopingComponent.DEFAULT_SLEEP_TIME));
    }
}
