package org.mybad.core.binary.animation;

import org.mybad.core.animation.Animation;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;
import org.mybad.core.binary.BinaryResourceSerializer;
import org.mybad.core.binary.BinaryResourceType;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes an entire animation JSON file (multiple clips) into a single payload.
 */
public final class AnimationSetBinarySerializer implements BinaryResourceSerializer<Map<String, Animation>> {

    private static final int VERSION = 1;
    private final AnimationBinarySerializer animationSerializer = new AnimationBinarySerializer();

    @Override
    public BinaryResourceType getType() {
        return BinaryResourceType.ANIMATION;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void write(BinaryDataWriter writer, Map<String, Animation> animations) throws IOException {
        Map<String, Animation> map = animations == null ? Collections.emptyMap() : animations;
        writer.writeVarInt(map.size());
        for (Map.Entry<String, Animation> entry : map.entrySet()) {
            writer.writeString(entry.getKey());
            animationSerializer.write(writer, entry.getValue());
        }
    }

    @Override
    public Map<String, Animation> read(BinaryDataReader reader) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        Map<String, Animation> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            Animation animation = animationSerializer.read(reader);
            if (name != null && animation != null) {
                map.put(name, animation);
            }
        }
        return map;
    }
}
