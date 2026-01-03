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
 * - Changed ModelTexture to native BedrockResourceLocation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.particle;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.component.ParticleComponent;
import org.mybad.bedrockparticle.particle.component.ParticleComponentParser;
import org.mybad.bedrockparticle.particle.event.ParticleEvent;
import org.mybad.bedrockparticle.particle.json.ParticleJsonTupleParser;
import org.mybad.bedrockparticle.particle.json.ParticleGsonHelper;
import org.mybad.bedrockparticle.particle.BedrockResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data about a particle from the Bedrock specs.
 *
// * @param description Basic info about the particle
// * @param curves      Custom curves to evaluate
// * @param events      Custom defined event listeners for when events are fired
// * @param components  All components in the particle
 * @author Ocelot
 * @since 1.0.0
 */
public final class ParticleData {

    Description description;
    Map<String, Curve> curves;
    Map<String, ParticleEvent> events;
    Map<String, ParticleComponent> components;

    public ParticleData(Description description, Map<String, Curve> curves, Map<String, ParticleEvent> events, Map<String, ParticleComponent> components) {
        this.description = description;
        this.curves = curves;
        this.events = events;
        this.components = components;
    }

    public void setTexture(BedrockResourceLocation resourceLocation){
        this.description = new Description(description.getIdentifier(), resourceLocation, description.getMaterial());
    }

    public Description description() {
        return this.description;
    }

    public Map<String, Curve> curves() {
        return this.curves;
    }

    public Map<String, ParticleEvent> events() {
        return this.events;
    }

    public Map<String, ParticleComponent> components() {
        return this.components;
    }



    private static final BedrockResourceLocation MISSING_TEXTURE = new BedrockResourceLocation("missingno");
    public static final ParticleData EMPTY = new ParticleData(new Description("empty", MISSING_TEXTURE, null), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());

    /**
     * The different types of curves for calculating particle variables.
     *
     * @author Ocelot
     */
    public enum CurveType {

        LINEAR, BEZIER, BEZIER_CHAIN, CATMULL_ROM

    }

    /**
     * Information about the particle.
     *
     * @author Ocelot
     */
    public static final class Description {
        private final String identifier;
        private final BedrockResourceLocation texture;
        private final String material;

        public Description(String identifier, BedrockResourceLocation texture, @Nullable String material) {
            this.identifier = identifier;
            this.texture = texture;
            this.material = material;
        }

        public String getIdentifier() {
            return identifier;
        }

        public BedrockResourceLocation getTexture() {
            return texture;
        }

        @Nullable
        public String getMaterial() {
            return material;
        }

        public static class Deserializer implements JsonDeserializer<Description> {

            @Override
            public Description deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = ParticleGsonHelper.convertToJsonObject(json, "description");
                String identifier = ParticleGsonHelper.getAsString(jsonObject, "identifier");
                JsonObject basicRenderParams = ParticleGsonHelper.getAsJsonObject(jsonObject, "basic_render_parameters");

                BedrockResourceLocation texture;
                if (basicRenderParams.has("texture")) {
                    String textureText = basicRenderParams.get("texture").getAsString();
                    if(!textureText.endsWith(".png")){
                        textureText += ".png";
                    }
                    texture = BedrockResourceLocation.tryParse(textureText);
                    if (texture == null) {
                        texture = MISSING_TEXTURE;
                    }
                } else {
                    texture = MISSING_TEXTURE;
                }
                String material = ParticleGsonHelper.getAsString(basicRenderParams, "material", null);

                return new Description(identifier, texture, material);
            }
        }
    }


    public static final class Curve {
        private final CurveType type;
        private final CurveNode[] nodes;
        private final MolangExpression input;
        private final MolangExpression horizontalRange;

        public Curve(CurveType type, CurveNode[] nodes, MolangExpression input, MolangExpression horizontalRange) {
            this.type = type;
            this.nodes = nodes;
            this.input = input;
            this.horizontalRange = horizontalRange;
        }

        public CurveType type() {
            return type;
        }

        public CurveNode[] nodes() {
            return nodes;
        }

        public MolangExpression input() {
            return input;
        }

        public MolangExpression horizontalRange() {
            return horizontalRange;
        }

        @Override
        public String toString() {
            return "Curve[type=" + this.type +
                    ", nodes=" + Arrays.toString(this.nodes) +
                    ", input=" + this.input +
                    ", horizontalRange=" + this.horizontalRange + "]";
        }

        public static class Deserializer implements JsonDeserializer<Curve> {

            private static CurveType parseType(JsonElement json) throws JsonParseException {
                if (!json.isJsonPrimitive()) {
                    throw new JsonSyntaxException("Expected String, was " + ParticleGsonHelper.getType(json));
                }
                for (CurveType curveType : CurveType.values()) {
                    if (curveType.name().equalsIgnoreCase(json.getAsString())) {
                        return curveType;
                    }
                }
                throw new JsonSyntaxException("Unsupported curve type: " + json.getAsString() + ". Supported curve types: " +
                        Arrays.stream(CurveType.values())
                                .map(type -> type.name().toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(", ")));
            }

            private static CurveNode[] parseNodes(JsonElement json, CurveType type) {
                if (json.isJsonArray()) {
                    if (type == CurveType.BEZIER_CHAIN) {
                        throw new JsonSyntaxException("Bezier Chain expected JsonObject, was " + ParticleGsonHelper.getType(json));
                    }

                    JsonArray array = ParticleGsonHelper.convertToJsonArray(json, "nodes");
                    CurveNode[] nodes = new CurveNode[array.size()];
                    int offset = type == CurveType.CATMULL_ROM ? 1 : 0;
                    for (int i = 0; i < nodes.length; i++) {
                        float time = (float) Math.max(i - offset, 0) / (float) (nodes.length - offset * 2 - 1);
                        MolangExpression value = ParticleJsonTupleParser.parseExpression(array.get(i), "nodes[" + i + "]");
                        nodes[i] = new CurveNode(time, value);
                    }
                    return nodes;
                } else if (json.isJsonObject()) {
                    JsonObject jsonObject = ParticleGsonHelper.convertToJsonObject(json, "nodes");
                    List<CurveNode> curveNodes = new ArrayList<>(jsonObject.entrySet().size());
                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        try {
                            float time = Float.parseFloat(entry.getKey());
                            JsonObject nodeJson = ParticleGsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());

                            if (type == CurveType.BEZIER_CHAIN) {
                                boolean singleValue = nodeJson.has("value");
                                boolean singleSlope = nodeJson.has("slope");
                                if (singleValue && (nodeJson.has("left_value") || nodeJson.has("right_value"))) {
                                    throw new JsonSyntaxException("left_value and right_value must not be present with value");
                                }
                                if (singleSlope && (nodeJson.has("left_slope") || nodeJson.has("right_slope"))) {
                                    throw new JsonSyntaxException("left_slope and right_slope must not be present with slope");
                                }

                                MolangExpression leftValue = singleValue ? ParticleJsonTupleParser.parseExpression(nodeJson.get("value"), "value") : ParticleJsonTupleParser.parseExpression(nodeJson.get("left_value"), "left_value");
                                MolangExpression rightValue = singleValue ? leftValue : ParticleJsonTupleParser.parseExpression(nodeJson.get("right_value"), "right_value");
                                MolangExpression leftSlope = singleSlope ? ParticleJsonTupleParser.parseExpression(nodeJson.get("slope"), "slope") : ParticleJsonTupleParser.parseExpression(nodeJson.get("left_slope"), "left_slope");
                                MolangExpression rightSlope = singleSlope ? leftSlope : ParticleJsonTupleParser.parseExpression(nodeJson.get("right_slope"), "right_slope");
                                curveNodes.add(new BezierChainCurveNode(time, leftValue, rightValue, leftSlope, rightSlope));
                            } else {
                                MolangExpression value = ParticleJsonTupleParser.parseExpression(nodeJson.get("value"), "value");
                                curveNodes.add(new CurveNode(time, value));
                            }
                        } catch (NumberFormatException e) {
                            throw new JsonParseException("Failed to parse nodes at time '" + entry.getKey() + "'", e);
                        }
                    }
                    if (type == CurveType.BEZIER && curveNodes.size() != 4) {
                        throw new JsonSyntaxException("Bezier expected 4 nodes, had " + curveNodes.size());
                    }
                    curveNodes.sort((a, b) -> Float.compare(a.getTime(), b.getTime()));
                    return curveNodes.toArray(new CurveNode[curveNodes.size()]);
                }
                throw new JsonSyntaxException("Expected nodes to be a JsonArray or JsonObject, was " + ParticleGsonHelper.getType(json));
            }

            @Override
            public Curve deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = ParticleGsonHelper.convertToJsonObject(json, "curve");
                CurveType type = parseType(jsonObject.get("type"));
                CurveNode[] curves = jsonObject.has("nodes") ? parseNodes(jsonObject.get("nodes"), type) : new CurveNode[0];
                MolangExpression input = ParticleJsonTupleParser.getExpression(jsonObject, "input", null);
                MolangExpression horizontalRange = ParticleJsonTupleParser.getExpression(jsonObject, "horizontal_range", () -> MolangExpression.of(1.0F));
                return new Curve(type, curves, input, horizontalRange);
            }
        }
    }

    /**
     * A node in a {@link Curve}. Used to define most types of curves.
     */
    public static class CurveNode {

        private final float time;
        private final MolangExpression value;

        public CurveNode(float time, MolangExpression value) {
            this.time = time;
            this.value = value;
        }

        public float getTime() {
            return this.time;
        }

        public MolangExpression getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CurveNode curveNode = (CurveNode) o;
            return Float.compare(curveNode.time, this.time) == 0 &&
                    this.value.equals(curveNode.value);
        }

        @Override
        public int hashCode() {
            int result = (this.time != 0.0f ? Float.floatToIntBits(this.time) : 0);
            result = 31 * result + this.value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CurveNode[" +
                    "time=" + this.time +
                    ", value=" + this.value + ']';
        }
    }

    /**
     * A node in a {@link Curve}. Used to define bezier chains specifically.
     */
    public static class BezierChainCurveNode extends CurveNode {

        private final MolangExpression leftValue;
        private final MolangExpression rightValue;
        private final MolangExpression leftSlope;
        private final MolangExpression rightSlope;

        public BezierChainCurveNode(float time, MolangExpression leftValue, MolangExpression rightValue, MolangExpression leftSlope, MolangExpression rightSlope) {
            super(time, leftValue);
            this.leftValue = leftValue;
            this.rightValue = rightValue;
            this.leftSlope = leftSlope;
            this.rightSlope = rightSlope;
        }

        public MolangExpression getLeftValue() {
            return this.leftValue;
        }

        public MolangExpression getRightValue() {
            return this.rightValue;
        }

        public MolangExpression getLeftSlope() {
            return this.leftSlope;
        }

        public MolangExpression getRightSlope() {
            return this.rightSlope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            BezierChainCurveNode that = (BezierChainCurveNode) o;
            return this.leftValue.equals(that.leftValue) &&
                    this.rightValue.equals(that.rightValue) &&
                    this.leftSlope.equals(that.leftSlope) &&
                    this.rightSlope.equals(that.rightSlope);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + this.leftValue.hashCode();
            result = 31 * result + this.rightValue.hashCode();
            result = 31 * result + this.leftSlope.hashCode();
            result = 31 * result + this.rightSlope.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "BezierChainCurveNode{" +
                    "time=" + this.getTime() +
                    ", leftValue=" + this.leftValue +
                    ", rightValue=" + this.rightValue +
                    ", leftSlope=" + this.leftSlope +
                    ", rightSlope=" + this.rightSlope +
                    '}';
        }
    }

    public static class Deserializer implements JsonDeserializer<ParticleData> {

        @Override
        public ParticleData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            final Description description = context.deserialize(jsonObject.get("description"), Description.class);

            ImmutableMap.Builder<String, Curve> curves = ImmutableMap.builder();
            if (jsonObject.has("curves")) {
                JsonObject curvesJson = ParticleGsonHelper.getAsJsonObject(jsonObject, "curves");
                for (Map.Entry<String, JsonElement> entry : curvesJson.entrySet()) {
                    String key = entry.getKey();
                    if (!key.startsWith("variable.") && !key.startsWith("v.")) {
                        throw new JsonSyntaxException(key + " is not a valid MoLang variable name");
                    }
                    curves.put(key.split("\\.", 2)[1], context.deserialize(entry.getValue(), Curve.class));
                }
            }

            ImmutableMap.Builder<String, ParticleEvent> events = ImmutableMap.builder();
            if (jsonObject.has("events")) {
                JsonObject eventsJson = ParticleGsonHelper.getAsJsonObject(jsonObject, "events");
                for (Map.Entry<String, JsonElement> entry : eventsJson.entrySet()) {
                    events.put(entry.getKey(), context.deserialize(entry.getValue(), ParticleEvent.class));
                }
            }

            Map<String, ParticleComponent> components;
            if (jsonObject.has("components")) {
                JsonObject eventsJson = ParticleGsonHelper.getAsJsonObject(jsonObject, "components");
                components = ParticleComponentParser.getInstance().deserialize(eventsJson);
            } else {
                components = Collections.emptyMap();
            }

            return new ParticleData(description, curves.build(), events.build(), components);
        }
    }
}
