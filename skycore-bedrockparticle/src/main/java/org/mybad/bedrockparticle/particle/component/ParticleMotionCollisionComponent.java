/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'org.mybad.bedrockparticle.particle.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.particle.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.json.ParticleJsonTupleParser;
import org.mybad.bedrockparticle.particle.json.ParticleGsonHelper;

/**
 * Component that specifies how a particle moves after colliding.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleMotionCollisionComponent implements ParticleComponent {

    private final MolangExpression enabled;
    private final float collisionDrag;
    private final float coefficientOfRestitution;
    private final float collisionRadius;
    private final boolean expireOnContact;
    private final String[] events;

    public ParticleMotionCollisionComponent(MolangExpression enabled,
                                            float collisionDrag,
                                            float coefficientOfRestitution,
                                            float collisionRadius,
                                            boolean expireOnContact,
                                            String[] events) {
        this.enabled = enabled;
        this.collisionDrag = collisionDrag;
        this.coefficientOfRestitution = coefficientOfRestitution;
        this.collisionRadius = collisionRadius;
        this.expireOnContact = expireOnContact;
        this.events = events;
    }

    public MolangExpression enabled() {
        return enabled;
    }

    public float collisionDrag() {
        return collisionDrag;
    }

    public float coefficientOfRestitution() {
        return coefficientOfRestitution;
    }

    public float collisionRadius() {
        return collisionRadius;
    }

    public boolean expireOnContact() {
        return expireOnContact;
    }

    public String[] events() {
        return events;
    }

    public static final MolangExpression DEFAULT_ENABLED = MolangExpression.of(true);

    public static ParticleMotionCollisionComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        MolangExpression enabled = ParticleJsonTupleParser.getExpression(jsonObject, "enabled", () -> DEFAULT_ENABLED);
        float collisionDrag = ParticleGsonHelper.getAsFloat(jsonObject, "collision_drag", 0) / 20F;
        float coefficientOfRestitution = ParticleGsonHelper.getAsFloat(jsonObject, "coefficient_of_restitution", 0);
        float collisionRadius = ParticleGsonHelper.getAsFloat(jsonObject, "collision_radius", 0.1F);
        boolean expireOnContact = ParticleGsonHelper.getAsBoolean(jsonObject, "expire_on_contact", false);
        String[] events = ParticleComponent.getEvents(jsonObject, "events");
        return new ParticleMotionCollisionComponent(enabled, collisionDrag, coefficientOfRestitution, collisionRadius, expireOnContact, events);
    }
}
