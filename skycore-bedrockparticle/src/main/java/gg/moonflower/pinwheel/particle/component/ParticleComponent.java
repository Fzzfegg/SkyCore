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

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a component in a particle definition that can be used to create actual Artemis entity components.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface ParticleComponent {

    /**
     * @return 该组件是否只能添加到发射器中
     */
    default boolean isEmitterComponent() {
        return false;
    }

    /**
     * @return 这个组件是否只能添加到粒子中
     */
    default boolean isParticleComponent() {
        return !this.isEmitterComponent();
    }

    /**
     * @return 当发射器循环时是否应该重新添加此组件
     */
    default boolean canLoop() {
        return false;
    }

    /**
     * 读取指定json中的所有事件引用。
     *
     * @param json The json to read references from
     * @param name The name of the element to get
     * @return The events parsed
     * @throws JsonSyntaxException If the file is malformed
     */
    static String[] getEvents(JsonObject json, String name) throws JsonSyntaxException {
        if (!json.has(name)) {
            return new String[0];
        }
        return ParticleComponent.parseEvents(json.get(name), name);
    }

    /**
     * 读取指定json中的所有事件引用。
     *
     * @param element The element to get as events
     * @param name    The name of the element
     * @return The events parsed
     * @throws JsonSyntaxException If the file is malformed
     */
    static String[] parseEvents(@Nullable JsonElement element, String name) throws JsonSyntaxException {
        if (element == null) {
            throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray or string");
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            String[] events = new String[array.size()];
            for (int i = 0; i < array.size(); i++) {
                events[i] = PinwheelGsonHelper.convertToString(array.get(i), name + "[" + i + "]");
            }
            return events;
        } else if (element.isJsonPrimitive()) {
            return new String[]{PinwheelGsonHelper.convertToString(element, name)};
        }
        throw new JsonSyntaxException("Expected " + name + " to be a JsonArray or string, was " + PinwheelGsonHelper.getType(element));
    }

    /**
     * 将方向解析为向内或自定义MoLang表达式。
     *
     * @param json The json to get the direction from
     * @param name The name of the element
     * @return The parsed direction
     */
    static Either<Boolean, MolangExpression[]> parseDirection(JsonObject json, String name) {
        if (!json.has(name)) {
            return Either.left(false);
        }

        if (json.get(name).isJsonPrimitive()) {
            String directionString = PinwheelGsonHelper.getAsString(json, name);
            if ("inwards".equalsIgnoreCase(directionString)) {
                return Either.left(true);
            } else if ("outwards".equalsIgnoreCase(directionString)) {
                return Either.left(false);
            } else {
                throw new JsonSyntaxException("Expected direction to be inwards or outwards, was " + directionString);
            }
        }

        return Either.right(JsonTupleParser.getExpression(json, name, 3, null));
    }

    /**
     * 向实体添加组件的上下文。
     */
    interface Context {

        /**
         * @return 运行事件的环境
         */
        MolangEnvironment getEnvironment();
    }

    /**
     * 从JSON中反序列化组件。
     */
    @FunctionalInterface
    interface Factory {

        /**
         * 从JSON中创建一个新的粒子组件。
         *
         * @param json The json to deserialize data from
         * @return A new particle component
         * @throws JsonParseException If any error occurs while deserializing the component
         */
        ParticleComponent create(JsonElement json) throws JsonParseException;
    }
}
