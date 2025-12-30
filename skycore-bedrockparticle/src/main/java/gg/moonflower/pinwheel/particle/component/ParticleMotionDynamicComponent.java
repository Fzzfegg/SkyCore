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
 * Component that specifies how a particle accelerates over time.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleMotionDynamicComponent implements ParticleComponent {

    private final MolangExpression[] linearAcceleration;
    private final MolangExpression linearDragCoefficient;
    private final MolangExpression rotationAcceleration;
    private final MolangExpression rotationDragCoefficient;

    public ParticleMotionDynamicComponent(MolangExpression[] linearAcceleration,
                                          MolangExpression linearDragCoefficient,
                                          MolangExpression rotationAcceleration,
                                          MolangExpression rotationDragCoefficient) {
        this.linearAcceleration = linearAcceleration;
        this.linearDragCoefficient = linearDragCoefficient;
        this.rotationAcceleration = rotationAcceleration;
        this.rotationDragCoefficient = rotationDragCoefficient;
    }

    public MolangExpression[] linearAcceleration() {
        return linearAcceleration;
    }

    public MolangExpression linearDragCoefficient() {
        return linearDragCoefficient;
    }

    public MolangExpression rotationAcceleration() {
        return rotationAcceleration;
    }

    public MolangExpression rotationDragCoefficient() {
        return rotationDragCoefficient;
    }

    public static final MolangExpression DEFAULT_LINEAR_ACCELERATION_X = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_LINEAR_ACCELERATION_Y = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_LINEAR_ACCELERATION_Z = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_LINEAR_DRAG_COEFFICIENT = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_ROTATION_ACCELERATION = MolangExpression.ZERO;
    public static final MolangExpression DEFAULT_ROTATION_DRAG_COEFFICIENT = MolangExpression.ZERO;

    public static ParticleMotionDynamicComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        MolangExpression[] linearAcceleration = JsonTupleParser.getExpression(object, "linear_acceleration", 3, () -> new MolangExpression[]{
                ParticleMotionDynamicComponent.DEFAULT_LINEAR_ACCELERATION_X,
                ParticleMotionDynamicComponent.DEFAULT_LINEAR_ACCELERATION_Y,
                ParticleMotionDynamicComponent.DEFAULT_LINEAR_ACCELERATION_Z
        });
        MolangExpression linearDragCoefficient = JsonTupleParser.getExpression(object, "linear_drag_coefficient", () -> ParticleMotionDynamicComponent.DEFAULT_LINEAR_DRAG_COEFFICIENT);
        MolangExpression rotationAcceleration = JsonTupleParser.getExpression(object, "rotation_acceleration", () -> ParticleMotionDynamicComponent.DEFAULT_ROTATION_ACCELERATION);
        MolangExpression rotationDragCoefficient = JsonTupleParser.getExpression(object, "rotation_drag_coefficient", () -> ParticleMotionDynamicComponent.DEFAULT_ROTATION_DRAG_COEFFICIENT);
        return new ParticleMotionDynamicComponent(linearAcceleration, linearDragCoefficient, rotationAcceleration, rotationDragCoefficient);
    }
}
