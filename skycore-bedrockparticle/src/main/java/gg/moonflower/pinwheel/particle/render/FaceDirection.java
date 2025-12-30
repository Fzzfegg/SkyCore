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
package gg.moonflower.pinwheel.particle.render;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Locale;

/**
 * The six directions a face can be textured in for cubes.
 *
 * @since 1.0.0
 */
public enum FaceDirection {

    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int data3d;
    private final String name;
    private final Vector3fc normal;

    FaceDirection(float x, float y, float z) {
        this.data3d = this.ordinal();
        this.name = this.name().toLowerCase(Locale.ROOT);
        this.normal = new Vector3f(x, y, z);
    }

    /**
     * @return The 3D data of this direction
     */
    public int get3DDataValue() {
        return this.data3d;
    }

    /**
     * @return The name of this direction
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The normal vector for this face
     */
    public Vector3fc normal() {
        return this.normal;
    }

    /**
     * @return The direction opposite to this direction
     */
    public FaceDirection opposite() {
        switch (this) {
            case DOWN:
                return UP;
            case UP:
                return DOWN;
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case EAST:
                return WEST;
            default:
                return this;
        }
    }
}
