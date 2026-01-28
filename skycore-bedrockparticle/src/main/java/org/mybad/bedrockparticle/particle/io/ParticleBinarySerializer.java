package org.mybad.bedrockparticle.particle.io;

import org.mybad.bedrockparticle.particle.BedrockResourceLocation;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.ParticleParser;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;
import org.mybad.core.binary.BinaryResourceSerializer;
import org.mybad.core.binary.BinaryResourceType;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binary serializer for Bedrock {@link ParticleData} definitions.
 */
public final class ParticleBinarySerializer implements BinaryResourceSerializer<ParticleData> {
    private static final int VERSION = 1;

    @Override
    public BinaryResourceType getType() {
        return BinaryResourceType.PARTICLE;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void write(BinaryDataWriter writer, ParticleData particle) throws IOException {
        if (particle == null) {
            writeDescription(writer, null);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            return;
        }
        writeDescription(writer, particle.description());
        writeCurves(writer, particle.curves());
        writeEvents(writer, particle.events());
        writeComponents(writer, particle.components());
    }

    @Override
    public ParticleData read(BinaryDataReader reader) throws IOException {
        ParticleData.Description description = readDescription(reader);
        Map<String, ParticleData.Curve> curves = readCurves(reader);
        Map<String, org.mybad.bedrockparticle.particle.event.ParticleEvent> events = readEvents(reader);
        Map<String, org.mybad.bedrockparticle.particle.component.ParticleComponent> components = readComponents(reader);
        return new ParticleData(description, curves, events, components);
    }

    private void writeDescription(BinaryDataWriter writer, ParticleData.Description description) throws IOException {
        writer.writeString(description == null ? "" : description.getIdentifier());
        writeResourceLocation(writer, description == null ? null : description.getTexture());
        writer.writeString(description == null ? "" : safe(description.getMaterial()));
        writer.writeBoolean(description != null && description.isBloom());
        writer.writeFloat(description == null ? 0f : description.getBloomStrength());
        writer.writeVarInt(description == null ? 0 : description.getBloomPasses());
        writer.writeFloat(description == null ? 0.06f : description.getBloomScaleStep());
        writer.writeFloat(description == null ? 1.0f : description.getBloomDownscale());
        writeResourceLocation(writer, description == null ? null : description.getEmissiveTexture());
        writer.writeFloat(description == null ? 0f : description.getEmissiveStrength());
        writeResourceLocation(writer, description == null ? null : description.getBlendTexture());
        writer.writeString(description == null ? "alpha" : safe(description.getBlendMode()));
        writer.writeFloatArray(description == null ? null : description.getBlendColor());
    }

    private ParticleData.Description readDescription(BinaryDataReader reader) throws IOException {
        String identifier = reader.readString();
        BedrockResourceLocation texture = readResourceLocation(reader);
        String material = reader.readString();
        boolean bloom = reader.readBoolean();
        float bloomStrength = reader.readFloat();
        int bloomPasses = reader.readVarInt();
        float bloomScaleStep = reader.readFloat();
        float bloomDownscale = reader.readFloat();
        BedrockResourceLocation emissive = readResourceLocation(reader);
        float emissiveStrength = reader.readFloat();
        BedrockResourceLocation blendTexture = readResourceLocation(reader);
        String blendMode = reader.readString();
        float[] blendColor = reader.readFloatArray();
        return new ParticleData.Description(
            identifier == null ? "" : identifier,
            texture,
            material == null || material.isEmpty() ? null : material,
            bloom,
            bloomStrength,
            bloomPasses,
            bloomScaleStep,
            bloomDownscale,
            emissive,
            emissiveStrength,
            blendTexture,
            blendMode == null || blendMode.isEmpty() ? "alpha" : blendMode,
            blendColor.length == 0 ? null : blendColor
        );
    }

    private void writeCurves(BinaryDataWriter writer, Map<String, ParticleData.Curve> curves) throws IOException {
        if (curves == null || curves.isEmpty()) {
            writer.writeVarInt(0);
            return;
        }
        writer.writeVarInt(curves.size());
        for (Map.Entry<String, ParticleData.Curve> entry : curves.entrySet()) {
            writer.writeString(entry.getKey());
            CurveSerializer.writeCurve(writer, entry.getValue());
        }
    }

    private Map<String, ParticleData.Curve> readCurves(BinaryDataReader reader) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        Map<String, ParticleData.Curve> curves = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            ParticleData.Curve curve = CurveSerializer.readCurve(reader);
            if (name != null && curve != null) {
                curves.put(name, curve);
            }
        }
        return curves;
    }

    private void writeEvents(BinaryDataWriter writer, Map<String, org.mybad.bedrockparticle.particle.event.ParticleEvent> events) throws IOException {
        if (events == null || events.isEmpty()) {
            writer.writeVarInt(0);
            return;
        }
        writer.writeVarInt(events.size());
        for (Map.Entry<String, org.mybad.bedrockparticle.particle.event.ParticleEvent> entry : events.entrySet()) {
            writer.writeString(entry.getKey());
            writer.writeString(ParticleParser.GSON.toJson(entry.getValue()));
        }
    }

    private Map<String, org.mybad.bedrockparticle.particle.event.ParticleEvent> readEvents(BinaryDataReader reader) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        Map<String, org.mybad.bedrockparticle.particle.event.ParticleEvent> events = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            String json = reader.readString();
            if (name != null && json != null) {
                events.put(name, ParticleParser.GSON.fromJson(json, org.mybad.bedrockparticle.particle.event.ParticleEvent.class));
            }
        }
        return events;
    }

    private void writeComponents(BinaryDataWriter writer, Map<String, org.mybad.bedrockparticle.particle.component.ParticleComponent> components) throws IOException {
        if (components == null || components.isEmpty()) {
            writer.writeVarInt(0);
            return;
        }
        writer.writeVarInt(components.size());
        for (Map.Entry<String, org.mybad.bedrockparticle.particle.component.ParticleComponent> entry : components.entrySet()) {
            writer.writeString(entry.getKey());
            writer.writeString(ParticleParser.GSON.toJson(entry.getValue()));
        }
    }

    private Map<String, org.mybad.bedrockparticle.particle.component.ParticleComponent> readComponents(BinaryDataReader reader) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        Map<String, org.mybad.bedrockparticle.particle.component.ParticleComponent> components = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            String json = reader.readString();
            if (name != null && json != null) {
                components.put(name, ParticleParser.GSON.fromJson(json, org.mybad.bedrockparticle.particle.component.ParticleComponent.class));
            }
        }
        return components;
    }

    private void writeResourceLocation(BinaryDataWriter writer, BedrockResourceLocation location) throws IOException {
        writer.writeString(location == null ? "" : location.toString());
    }

    private BedrockResourceLocation readResourceLocation(BinaryDataReader reader) throws IOException {
        String text = reader.readString();
        if (text == null || text.isEmpty()) {
            return null;
        }
        return BedrockResourceLocation.tryParse(text);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
