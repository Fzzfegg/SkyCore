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

import gg.moonflower.pinwheel.particle.ParticleContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Executes a random {@link ParticleEvent} based on weight.
 */
public class RandomParticleEvent implements ParticleEvent {

    private final List<WeightedEvent> events;
    private final int totalWeight;

    public RandomParticleEvent(ParticleEvent[] events, int[] weights) {
        if (events.length != weights.length) {
            throw new IllegalArgumentException("Expected " + events.length + " weights, got " + weights.length);
        }
        this.events = IntStream.range(0, events.length)
                .mapToObj(i -> new WeightedEvent(events[i], weights[i]))
                .collect(Collectors.toList());

        long weight = 0L;
        for (WeightedEvent weightedEntry : this.events) {
            weight += weightedEntry.weight;
        }

        if (weight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        }
        this.totalWeight = (int) weight;
    }

    @Override
    public void execute(ParticleContext context) {
        if (this.events.isEmpty()) {
            return;
        }

        int roll = context.getRandom().nextInt(this.totalWeight);
        for (WeightedEvent weightedEntry : this.events) {
            roll -= weightedEntry.weight;
            if (roll < 0) {
                weightedEntry.event.execute(context);
                break;
            }
        }
    }

    private static final class WeightedEvent {
        private final ParticleEvent event;
        private final int weight;

        private WeightedEvent(ParticleEvent event, int weight) {
            this.event = event;
            this.weight = weight;
        }
    }
}
