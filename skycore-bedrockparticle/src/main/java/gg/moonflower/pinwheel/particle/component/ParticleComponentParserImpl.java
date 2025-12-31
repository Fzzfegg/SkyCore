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
 * - Merged implementation from Pollen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package gg.moonflower.pinwheel.particle.component;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public final class ParticleComponentParserImpl {

    private static final Logger LOGGER = LogManager.getLogger(ParticleComponentParser.class);
    private static final ParticleComponentParser INSTANCE = ParticleComponentParserImpl::deserialize;
    private static final Map<String, ParticleComponent.Factory> FACTORIES;

    static {
        ImmutableMap.Builder<String, ParticleComponent.Factory> builder = ImmutableMap.builder();
        put(builder, "emitter_lifetime_events", ParticleLifetimeEventComponent::deserialize);
        put(builder, "emitter_lifetime_expression", EmitterLifetimeExpressionComponent::deserialize);
        put(builder, "emitter_lifetime_looping", EmitterLifetimeLoopingComponent::deserialize);
        put(builder, "emitter_lifetime_once", EmitterLifetimeOnceComponent::deserialize);

        put(builder, "emitter_rate_instant", EmitterRateInstantComponent::deserialize);
        // Omit emitter_rate_manual
        put(builder, "emitter_rate_steady", EmitterRateSteadyComponent::deserialize);

        put(builder, "emitter_shape_disc", EmitterShapeDiscComponent::deserialize);
        put(builder, "emitter_shape_box", EmitterShapeBoxComponent::deserialize);
        put(builder, "emitter_shape_custom", EmitterShapePointComponent::deserialize);
        put(builder, "emitter_shape_entity_aabb", EmitterShapeEntityBoxComponent::deserialize);
        put(builder, "emitter_shape_point", EmitterShapePointComponent::deserialize);
        put(builder, "emitter_shape_sphere", EmitterShapeSphereComponent::deserialize);

        put(builder, "emitter_initialization", EmitterInitializationComponent::deserialize);
        put(builder, "particle_initialization", EmitterInitializationComponent::deserialize);
        put(builder, "emitter_local_space", EmitterLocalSpaceComponent::deserialize);

        put(builder, "particle_appearance_billboard", ParticleAppearanceBillboardComponent::deserialize);
        put(builder, "particle_appearance_lighting", ParticleAppearanceLightingComponent::deserialize);
        put(builder, "particle_appearance_tinting", ParticleAppearanceTintingComponent::deserialize);

        put(builder, "particle_initial_speed", ParticleInitialSpeedComponent::deserialize);
        put(builder, "particle_initial_spin", ParticleInitialSpinComponent::deserialize);

        put(builder, "particle_expire_if_in_blocks", ParticleExpireInBlocksComponent::deserialize);
        put(builder, "particle_expire_if_not_in_blocks", ParticleExpireNotInBlocksComponent::deserialize);
        put(builder, "particle_lifetime_events", ParticleLifetimeEventComponent::deserialize);
        put(builder, "particle_lifetime_expression", ParticleLifetimeExpressionComponent::deserialize);
        put(builder, "particle_kill_plane", ParticleKillPlaneComponent::deserialize);

        put(builder, "particle_motion_collision", ParticleMotionCollisionComponent::deserialize);
        put(builder, "particle_motion_dynamic", ParticleMotionDynamicComponent::deserialize);
        put(builder, "particle_motion_parametric", ParticleMotionParametricComponent::deserialize);
        FACTORIES = builder.build();
    }

    private ParticleComponentParserImpl() {
    }

    private static void put(ImmutableMap.Builder<String, ParticleComponent.Factory> builder, String name, ParticleComponent.Factory factory) {
        builder.put("minecraft:" + name, factory);
    }

    public static Map<String, ParticleComponent> deserialize(JsonObject json) throws JsonParseException {
        Map<String, ParticleComponent> components = new HashMap<>(json.size());
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            String fullKey = key.contains(":") ? key : "minecraft:" + key;
            ParticleComponent.Factory factory = FACTORIES.get(fullKey);
            if (factory == null) {
                LOGGER.error("Unknown particle component: {}", fullKey);
                continue;
            }
            try {
                components.put(key, factory.create(entry.getValue()));
            } catch (Exception e) {
                LOGGER.error("Failed to parse component: {} - Cause: {}", key, e.getMessage(), e);
                String detail = e.getMessage();
                if (detail != null && !detail.isEmpty()) {
                    throw new JsonSyntaxException("Invalid particle component: " + key + " - " + detail, e);
                }
                throw new JsonSyntaxException("Invalid particle component: " + key, e);
            }
        }
        return Collections.unmodifiableMap(components);
    }

    public static ParticleComponentParser getInstance() {
        return INSTANCE;
    }
}
