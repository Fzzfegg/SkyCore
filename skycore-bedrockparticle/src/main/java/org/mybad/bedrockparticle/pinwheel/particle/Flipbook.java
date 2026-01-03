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
package org.mybad.bedrockparticle.pinwheel.particle;

import com.google.gson.*;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.json.JsonTupleParser;
import org.mybad.bedrockparticle.pinwheel.particle.json.PinwheelGsonHelper;

import java.lang.reflect.Type;

/**
 * flipbook定义了一组uv如何随时间转换。
 *
 * @param baseU             The first frame x coordinate on the texture
 * @param baseV             The first frame y coordinate on the texture
 * @param sizeU             The x size of each frame
 * @param sizeV             The y size of each frame
 * @param stepU             The x step for each frame
 * @param stepV             The y step for each frame
 * @param fps               The number of frames to show per second
 * @param maxFrame          The maximum frame to show
 * @param stretchToLifetime Whether to set fps to match the lifetime
 * @param loop              Whether to loop when the last frame is reached
 * @author Ocelot
 * @since 1.0.0
 */
public final class Flipbook {

    private final MolangExpression baseU;
    private final MolangExpression baseV;
    private final float sizeU;
    private final float sizeV;
    private final float stepU;
    private final float stepV;
    private final float fps;
    private final MolangExpression maxFrame;
    private final boolean stretchToLifetime;
    private final boolean loop;

    public Flipbook(MolangExpression baseU,
                    MolangExpression baseV,
                    float sizeU,
                    float sizeV,
                    float stepU,
                    float stepV,
                    float fps,
                    MolangExpression maxFrame,
                    boolean stretchToLifetime,
                    boolean loop) {
        this.baseU = baseU;
        this.baseV = baseV;
        this.sizeU = sizeU;
        this.sizeV = sizeV;
        this.stepU = stepU;
        this.stepV = stepV;
        this.fps = fps;
        this.maxFrame = maxFrame;
        this.stretchToLifetime = stretchToLifetime;
        this.loop = loop;
    }

    public MolangExpression baseU() {
        return baseU;
    }

    public MolangExpression baseV() {
        return baseV;
    }

    public float sizeU() {
        return sizeU;
    }

    public float sizeV() {
        return sizeV;
    }

    public float stepU() {
        return stepU;
    }

    public float stepV() {
        return stepV;
    }

    public float fps() {
        return fps;
    }

    public MolangExpression maxFrame() {
        return maxFrame;
    }

    public boolean stretchToLifetime() {
        return stretchToLifetime;
    }

    public boolean loop() {
        return loop;
    }

    public static class Deserializer implements JsonDeserializer<Flipbook> {

        @Override
        public Flipbook deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            MolangExpression[] uv = JsonTupleParser.getExpression(jsonObject, "base_UV", 2, null);
            float[] sizeUV = JsonTupleParser.getFloat(jsonObject, "size_UV", 2, null);
            float[] stepUV = JsonTupleParser.getFloat(jsonObject, "step_UV", 2, null);
            float fps = PinwheelGsonHelper.getAsFloat(jsonObject, "frames_per_second", 1);
            MolangExpression maxFrame = JsonTupleParser.getExpression(jsonObject, "max_frame", null);
            boolean stretchToLifetime = PinwheelGsonHelper.getAsBoolean(jsonObject, "stretch_to_lifetime", false);
            boolean loop = PinwheelGsonHelper.getAsBoolean(jsonObject, "loop", false);
            return new Flipbook(uv[0], uv[1], sizeUV[0], sizeUV[1], stepUV[0], stepUV[1], fps, maxFrame, stretchToLifetime, loop);
        }
    }
}
