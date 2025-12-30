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
import com.google.gson.JsonSyntaxException;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.pinwheel.particle.json.JsonTupleParser;
import gg.moonflower.pinwheel.particle.render.Flipbook;
import gg.moonflower.pinwheel.particle.ParticleInstance;
import gg.moonflower.pinwheel.particle.ParticleParser;
import gg.moonflower.pinwheel.particle.json.PinwheelGsonHelper;
import gg.moonflower.pollen.particle.render.QuadRenderProperties;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Component that specifies the billboard properties of a particle.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleAppearanceBillboardComponent implements ParticleComponent {

    private final MolangExpression[] size;
    private final FaceCameraMode cameraMode;
    private final float minSpeedThreshold;
    @Nullable
    private final MolangExpression[] customDirection;
    private final TextureSetter textureSetter;

    public ParticleAppearanceBillboardComponent(MolangExpression[] size,
                                                FaceCameraMode cameraMode,
                                                float minSpeedThreshold,
                                                @Nullable MolangExpression[] customDirection,
                                                TextureSetter textureSetter) {
        this.size = size;
        this.cameraMode = cameraMode;
        this.minSpeedThreshold = minSpeedThreshold;
        this.customDirection = customDirection;
        this.textureSetter = textureSetter;
    }

    public MolangExpression[] size() {
        return size;
    }

    public FaceCameraMode cameraMode() {
        return cameraMode;
    }

    public float minSpeedThreshold() {
        return minSpeedThreshold;
    }

    @Nullable
    public MolangExpression[] customDirection() {
        return customDirection;
    }

    public TextureSetter textureSetter() {
        return textureSetter;
    }

    public static final TextureSetter DEFAULT_UV = (particle, environment, properties) -> properties.setUV(0, 0, 1, 1);

    public static ParticleAppearanceBillboardComponent deserialize(JsonElement json) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        MolangExpression[] size = JsonTupleParser.getExpression(jsonObject, "size", 2, null);
        FaceCameraMode cameraMode = FaceCameraMode.parseCameraMode(PinwheelGsonHelper.getAsString(jsonObject, "facing_camera_mode", FaceCameraMode.ROTATE_XYZ.getName()));

        float minSpeedThreshold = 0.01F;
        MolangExpression[] customDirection = null;
        if (jsonObject.has("direction")) {
            JsonObject directionJson = PinwheelGsonHelper.getAsJsonObject(jsonObject, "direction");
            if ("custom_direction".equals(PinwheelGsonHelper.getAsString(directionJson, "mode"))) {
                customDirection = JsonTupleParser.getExpression(directionJson, "direction", 3, () -> new MolangExpression[]{
                        MolangExpression.ZERO,
                        MolangExpression.ZERO,
                        MolangExpression.ZERO
                });
            } else {
                minSpeedThreshold = PinwheelGsonHelper.getAsFloat(directionJson, "min_speed_threshold", minSpeedThreshold);
            }
        }

        TextureSetter textureSetter = ParticleAppearanceBillboardComponent.DEFAULT_UV;
        if (jsonObject.has("uv")) {
            JsonObject uvJson = PinwheelGsonHelper.getAsJsonObject(jsonObject, "uv");
            int textureWidth = PinwheelGsonHelper.getAsInt(uvJson, "texture_width", 1);
            int textureHeight = PinwheelGsonHelper.getAsInt(uvJson, "texture_height", 1);

            if (uvJson.has("flipbook")) {
                Flipbook flipbook = ParticleParser.parseFlipbook(uvJson.get("flipbook"));
                textureSetter = TextureSetter.flipbook(textureWidth, textureHeight, flipbook);
            } else {
                MolangExpression[] uv = JsonTupleParser.getExpression(uvJson, "uv", 2, null);
                MolangExpression[] uvSize = JsonTupleParser.getExpression(uvJson, "uv_size", 2, null);
                textureSetter = TextureSetter.constant(textureWidth, textureHeight, uv, uvSize);
            }
        }

        return new ParticleAppearanceBillboardComponent(size, cameraMode, minSpeedThreshold, customDirection, textureSetter);
    }


    /**
     * The different types of camera transforms for particles.
     *
     * @author Ocelot
     * @since 1.6.0
     */
    public enum FaceCameraMode {
        ROTATE_XYZ("rotate_xyz"),
        ROTATE_Y("rotate_y"),
        LOOK_AT_XYZ("lookat_xyz"),
        LOOK_AT_Y("lookat_y"),
        DIRECTION_X("direction_x"),
        DIRECTION_Y("direction_y"),
        DIRECTION_Z("direction_z"),

        LOOKAT_DIRECTION("lookat_direction"),
        EMITTER_TRANSFORM_XY("emitter_transform_xy"),
        EMITTER_TRANSFORM_XZ("emitter_transform_xz"),
        EMITTER_TRANSFORM_YZ("emitter_transform_yz");

        private final String name;

        FaceCameraMode(String name) {
            this.name = name;
        }

        /**
         * @return The JSON name of this camera mode
         */
        public String getName() {
            return this.name;
        }

        /**
         * Parses a camera mode from the specified name.
         *
         * @param type The type of mode to parse
         * @return The mode found
         * @throws JsonParseException If the mode does not exist
         */
        public static FaceCameraMode parseCameraMode(String type) throws JsonParseException {
            for (FaceCameraMode cameraMode : FaceCameraMode.values()) {
                if (cameraMode.name.equalsIgnoreCase(type)) {
                    return cameraMode;
                }
            }
            throw new JsonSyntaxException("Unsupported camera mode: " + type + ". Supported camera modes: " +
                    Arrays.stream(FaceCameraMode.values())
                            .map(FaceCameraMode::getName)
                            .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Setter for particle UV.
     */
    @FunctionalInterface
    public interface TextureSetter {

        /**
         * Sets the UV coordinate for the specified particle.
         *
         * @param particle    The particle to set the UV of
         * @param environment The environment to evaluate uvs in
         * @param properties  The render properties to set
         */
        void setUV(ParticleInstance particle, MolangEnvironment environment, QuadRenderProperties properties);

        /**
         * Creates a constant UV coordinate.
         *
         * @param textureWidth  The width of the texture
         * @param textureHeight The height of the texture
         * @param uv            The uv
         * @param uvSize        The size of the uv
         * @return A new constant uv setter
         */
        static TextureSetter constant(int textureWidth, int textureHeight, MolangExpression[] uv, MolangExpression[] uvSize) {
            return new ConstantTextureSetter(textureWidth, textureHeight, uv, uvSize);
        }

        /**
         * Creates a texture setter from a flipbook.
         *
         * @param textureWidth  The width of the texture
         * @param textureHeight The height of the texture
         * @param flipbook      The flipbook to animate through
         * @return A new flipbook uv setter
         */
        static TextureSetter flipbook(int textureWidth, int textureHeight, Flipbook flipbook) {
            return new FlipbookTextureSetter(textureWidth, textureHeight, flipbook);
        }
    }

    private static final class ConstantTextureSetter implements TextureSetter {
        private final int textureWidth;
        private final int textureHeight;
        private final MolangExpression[] uv;
        private final MolangExpression[] uvSize;

        private ConstantTextureSetter(int textureWidth,
                                      int textureHeight,
                                      MolangExpression[] uv,
                                      MolangExpression[] uvSize) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.uv = uv;
            this.uvSize = uvSize;
        }

        @Override
        public void setUV(ParticleInstance particle, MolangEnvironment environment, QuadRenderProperties properties) {
            float u0 = environment.safeResolve(this.uv[0]);
            float v0 = environment.safeResolve(this.uv[1]);
            float u1 = u0 + environment.safeResolve(this.uvSize[0]);
            float v1 = v0 + environment.safeResolve(this.uvSize[1]);
            properties.setUV(u0 / (float) this.textureWidth, v0 / (float) this.textureHeight, u1 / (float) this.textureWidth, v1 / (float) this.textureHeight);
        }
    }

    private static final class FlipbookTextureSetter implements TextureSetter {
        private final int textureWidth;
        private final int textureHeight;
        private final Flipbook flipbook;

        private FlipbookTextureSetter(int textureWidth,
                                      int textureHeight,
                                      Flipbook flipbook) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.flipbook = flipbook;
        }

        @Override
        public void setUV(ParticleInstance particle, MolangEnvironment environment, QuadRenderProperties properties) {
            float age = particle.getParticleAge();
            float life = particle.getParticleLifetime();
            properties.setUV(environment, this.textureWidth, this.textureHeight, this.flipbook, age, life);
        }
    }
}
