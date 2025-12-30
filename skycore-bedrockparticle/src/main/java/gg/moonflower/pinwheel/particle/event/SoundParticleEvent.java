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
package gg.moonflower.pinwheel.particle.event;

import com.google.gson.*;
import gg.moonflower.pinwheel.particle.ParticleContext;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;

import java.lang.reflect.Type;

/**
 * Creates a sound effect.
 *
 * @param effect The sound id to play
 */
public final class SoundParticleEvent implements ParticleEvent {

    private final String effect;

    public SoundParticleEvent(String effect) {
        this.effect = effect;
    }

    public String effect() {
        return effect;
    }

    @Override
    public void execute(ParticleContext context) {
        context.soundEffect(this.effect);
    }

    public static class Deserializer implements JsonDeserializer<SoundParticleEvent> {

        @Override
        public SoundParticleEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = PinwheelGsonHelper.convertToJsonObject(json, "sound_effect");
            String effect = PinwheelGsonHelper.getAsString(jsonObject, "event_name");
            return new SoundParticleEvent(effect);
        }
    }
}
