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
import gg.moonflower.pinwheel.particle.ParticleContext;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an event a custom particle can invoke.
 *
 * @author Ocelot
 * @since 1.0.0
 */
@FunctionalInterface
public interface ParticleEvent {

    /**
     * Executes this event.
     *
     * @param context The context for execution
     */
    void execute(ParticleContext context);

    /**
     * Creates an event that executes each given event one after another.
     *
     * @param events The events to execute
     * @return The sequence event
     */
    static ParticleEvent sequence(Iterable<ParticleEvent> events) {
        return context -> events.forEach(event -> event.execute(context));
    }

    /**
     * Creates an event that executes each given event one after another.
     *
     * @param events The events to execute
     * @return The sequence event
     */
    static ParticleEvent sequence(ParticleEvent... events) {
        return context -> {
            for (ParticleEvent event : events) {
                event.execute(context);
            }
        };
    }

    /**
     * Creates an event that logs the given message.
     *
     * @param message The message to send
     * @return The log event
     */
    static ParticleEvent log(String message) {
        return context -> context.log(message);
    }

    /**
     * Creates an event that runs the given expression.
     *
     * @param expression The expression to evaluate
     * @return The expression event
     */
    static ParticleEvent expression(MolangExpression expression) {
        return context -> context.expression(expression);
    }

    /**
     * Types of spawning particles. This is to allow particles to behave differently depending on how they are spawned.
     */
    enum ParticleSpawnType {
        EMITTER, EMITTER_BOUND, PARTICLE, PARTICLE_WITH_VELOCITY
    }

    /**
     * JSON deserializer for particle events.
     */
    class Deserializer implements JsonDeserializer<ParticleEvent> {

        private static ParticleEvent[] parseSequence(@Nullable JsonElement element, JsonDeserializationContext context) {
            if (element == null) {
                return new ParticleEvent[0];
            }
            JsonArray array = PinwheelGsonHelper.convertToJsonArray(element, "sequence");
            if (array.size() == 0) {
                throw new JsonSyntaxException("Empty particle sequence event");
            }
            if (array.size() == 1) {
                return context.deserialize(array.get(0), ParticleEvent.class);
            }
            ParticleEvent[] events = new ParticleEvent[array.size()];
            for (int i = 0; i < array.size(); i++) {
                events[i] = context.deserialize(array.get(i), ParticleEvent.class);
            }
            return events;
        }

        private static @Nullable ParticleEvent parseRandom(@Nullable JsonElement element, JsonDeserializationContext context) {
            if (element == null) {
                return null;
            }
            JsonArray array = PinwheelGsonHelper.convertToJsonArray(element, "randomize");
            if (array.size() == 0) {
                throw new JsonSyntaxException("Empty particle randomize event");
            }
            ParticleEvent[] events = new ParticleEvent[array.size()];
            int[] weights = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                JsonObject arrayElement = PinwheelGsonHelper.convertToJsonObject(array.get(i), "randomize[" + i + "]");
                events[i] = context.deserialize(arrayElement, ParticleEvent.class);
                weights[i] = PinwheelGsonHelper.getAsInt(arrayElement, "weight", 0);
            }
            return new RandomParticleEvent(events, weights);
        }

        @Override
        public ParticleEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = PinwheelGsonHelper.convertToJsonObject(json, "event");
            ParticleEvent[] sequence = parseSequence(jsonObject.get("sequence"), context);
            ParticleEvent random = parseRandom(jsonObject.get("randomize"), context);

            List<ParticleEvent> events = new ArrayList<>(Arrays.asList(sequence));
            if (random != null) {
                events.add(random);
            }
            if (jsonObject.has("particle_effect")) {
                events.add(context.deserialize(jsonObject.get("particle_effect"), SpawnParticleEvent.class));
            }
            if (jsonObject.has("sound_effect")) {
                events.add(context.deserialize(jsonObject.get("sound_effect"), SoundParticleEvent.class));
            }
            if (jsonObject.has("expression")) {
                events.add(expression(JsonTupleParser.getExpression(jsonObject, "expression", null)));
            }
            if (jsonObject.has("log")) {
                events.add(log(PinwheelGsonHelper.getAsString(jsonObject, "log")));
            }
            if (events.isEmpty()) {
                throw new JsonSyntaxException("Empty event");
            }
            if (events.size() == 1) {
                return events.get(0);
            }
            return sequence(events);
        }
    }
}
