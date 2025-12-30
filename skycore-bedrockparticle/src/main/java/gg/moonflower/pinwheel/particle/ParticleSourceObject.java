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
package gg.moonflower.pinwheel.particle;

/**
 * An object particles can spawn from.
 *
 * @since 1.0.0
 */
public interface ParticleSourceObject {

    /**
     * @return The minimum x position of the AABB
     */
    float getMinX();

    /**
     * @return The minimum y position of the AABB
     */
    float getMinY();

    /**
     * @return The minimum z position of the AABB
     */
    float getMinZ();

    /**
     * @return The maximum x position of the AABB
     */
    float getMaxX();

    /**
     * @return The maximum y position of the AABB
     */
    float getMaxY();

    /**
     * @return The maximum z position of the AABB
     */
    float getMaxZ();
}
