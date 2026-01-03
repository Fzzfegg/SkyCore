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
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;

/**
 * Component that determines if position, rotation, and velocity are relative to the emitter reference.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class EmitterLocalSpaceComponent implements ParticleComponent {

    private final boolean position;
    private final boolean rotation;
    private final boolean velocity;

    public EmitterLocalSpaceComponent(boolean position, boolean rotation, boolean velocity) {
        this.position = position;
        this.rotation = rotation;
        this.velocity = velocity;
    }

    public boolean position() {
        return position;
    }

    public boolean rotation() {
        return rotation;
    }

    public boolean velocity() {
        return velocity;
    }

    public static EmitterLocalSpaceComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return new EmitterLocalSpaceComponent(
                PinwheelGsonHelper.getAsBoolean(object, "position", false),
                PinwheelGsonHelper.getAsBoolean(object, "rotation", false),
                PinwheelGsonHelper.getAsBoolean(object, "velocity", false));
    }
}
