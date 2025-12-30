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

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.pinwheel.particle.ParticleInstance;
import gg.moonflower.pinwheel.particle.ParticleSourceObject;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Components that can emit particles in different shapes.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface ParticleEmitterShape extends ParticleEmitterComponent {

    /**
     * 默认粒子发射器形状，不产生粒子。
     */
    ParticleEmitterShape EMPTY = (spawner, count) -> {
    };

    /**
     * 发出指定数量的粒子。
     *
     * @param spawner The target to create particles
     * @param count   The number of particles to spawn
     */
    void emitParticles(ParticleEmitterShape.Spawner spawner, int count);

    /**
     * 在世界中生成粒子并正确地连接它们.
     */
    interface Spawner {

        /**
         * @return 一个新的粒子实体
         */
        ParticleInstance createParticle();

        /**
         * 在世界中生成指定的粒子。
         *
         * @param particle The particle to spawn
         */
        void spawnParticle(ParticleInstance particle);

        /**
         * @return 生成实体实例或null（如果这个生成器没有附加到任何东西
         */
        @Nullable ParticleSourceObject getEntity();

        /**
         * @return 生成实例的环境
         */
        MolangEnvironment getEnvironment();

        /**
         * @return 粒子的随机实例
         */
        Random getRandom();

        /**
         * 设置指定粒子的相对位置。
         *
         * @param x The new x position
         * @param y The new y position
         * @param z The new z position
         */
        void setPosition(ParticleInstance particle, double x, double y, double z);

        /**
         * 设置指定粒子的速度。
         *
         * @param dx The new x velocity
         * @param dy The new y velocity
         * @param dz The new z velocity
         */
        void setVelocity(ParticleInstance particle, double dx, double dy, double dz);

        /**
         * 设置指定粒子的相对位置和速度。
         *
         * @param x  The new x position
         * @param y  The new y position
         * @param z  The new z position
         * @param dx The new x velocity
         * @param dy The new y velocity
         * @param dz The new z velocity
         */
        default void setPositionVelocity(ParticleInstance particle, double x, double y, double z, double dx, double dy, double dz) {
            this.setPosition(particle, x, y, z);
            this.setVelocity(particle, dx, dy, dz);
        }
    }
}
