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
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.ParticleContext;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Spawns a particle emitter.
 *
 * @param effect              The name of the particle to summon
 * @param type                The type of spawning to use
 * @param preEffectExpression The expression to evaluate before spawning the particle
 */
public final class SpawnParticleEvent implements ParticleEvent {

    private final String effect;
    private final ParticleEvent.ParticleSpawnType type;
    @Nullable
    private final MolangExpression preEffectExpression;

    public SpawnParticleEvent(String effect,
                              ParticleEvent.ParticleSpawnType type,
                              @Nullable MolangExpression preEffectExpression) {
        this.effect = effect;
        this.type = type;
        this.preEffectExpression = preEffectExpression;
    }

    public String effect() {
        return effect;
    }

    public ParticleEvent.ParticleSpawnType type() {
        return type;
    }

    @Nullable
    public MolangExpression preEffectExpression() {
        return preEffectExpression;
    }

    @Override
    public void execute(ParticleContext context) {
        if (this.preEffectExpression != null) {
            context.expression(this.preEffectExpression);
        }
        context.particleEffect(this.effect, this.type);
    }

    public static class Deserializer implements JsonDeserializer<SpawnParticleEvent> {

        private static ParticleSpawnType parseType(String name) throws JsonParseException {
            for (ParticleSpawnType curveType : ParticleSpawnType.values()) {
                if (curveType.name().toLowerCase(Locale.ROOT).equalsIgnoreCase(name)) {
                    return curveType;
                }
            }
            String types = Arrays.stream(ParticleSpawnType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            throw new JsonSyntaxException("Unsupported particle type: " + name + ". Supported particle types: " + types);
        }

        @Override
        public SpawnParticleEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = PinwheelGsonHelper.convertToJsonObject(json, "particle_effect");
            String effect = PinwheelGsonHelper.getAsString(jsonObject, "effect");
            ParticleSpawnType type = parseType(PinwheelGsonHelper.getAsString(jsonObject, "type"));
            MolangExpression expression = JsonTupleParser.getExpression(jsonObject, "pre_effect_expression", () -> null);
            return new SpawnParticleEvent(effect, type, expression);
        }
    }
}
