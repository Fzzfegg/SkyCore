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
import com.google.gson.JsonSyntaxException;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 监听生命周期事件的组件。
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleLifetimeEventComponent implements ParticleComponent {

    private final String[] creationEvent;
    private final String[] expirationEvent;
    private final TimelineEvent[] timelineEvents;

    public ParticleLifetimeEventComponent(String[] creationEvent,
                                          String[] expirationEvent,
                                          TimelineEvent[] timelineEvents) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timelineEvents = timelineEvents;
    }

    public String[] creationEvent() {
        return creationEvent;
    }

    public String[] expirationEvent() {
        return expirationEvent;
    }

    public TimelineEvent[] timelineEvents() {
        return timelineEvents;
    }

    public static ParticleLifetimeEventComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String[] creationEvent = ParticleComponent.getEvents(jsonObject, "creation_event");
        String[] expirationEvent = ParticleComponent.getEvents(jsonObject, "expiration_event");

        TimelineEvent[] timelineEvents;
        if (jsonObject.has("timeline")) {
            JsonObject timelineJson = PinwheelGsonHelper.getAsJsonObject(jsonObject, "timeline");
            List<TimelineEvent> events = new ArrayList<>(timelineJson.entrySet().size());
            for (Map.Entry<String, JsonElement> entry : timelineJson.entrySet()) {
                try {
                    events.add(new TimelineEvent(Float.parseFloat(entry.getKey()), ParticleComponent.parseEvents(entry.getValue(), entry.getKey())));
                } catch (Exception e) {
                    throw new JsonSyntaxException("Failed to parse " + entry.getKey() + " in timeline", e);
                }
            }
            events.sort((a, b) -> Float.compare(a.time(), b.time()));
            timelineEvents = events.toArray(new TimelineEvent[events.size()]);
        } else {
            timelineEvents = new TimelineEvent[0];
        }

        return new ParticleLifetimeEventComponent(creationEvent, expirationEvent, timelineEvents);
    }

    @Override
    public boolean isEmitterComponent() {
        return true;
    }

    @Override
    public boolean isParticleComponent() {
        return true;
    }

    /**
     * An event that occurs on the timeline.
     *
     * @param time   The time the event happens at
     * @param events The events to fire
     */
    public static final class TimelineEvent {
        private final float time;
        private final String[] events;

        public TimelineEvent(float time, String[] events) {
            this.time = time;
            this.events = events;
        }

        public float time() {
            return time;
        }

        public String[] events() {
            return events;
        }
    }
}
