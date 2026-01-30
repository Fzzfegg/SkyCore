package org.mybad.core.binary.animation;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.Interpolation;
import org.mybad.core.animation.InterpolationImpl;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;
import org.mybad.core.binary.BinaryResourceSerializer;
import org.mybad.core.binary.BinaryResourceType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Binary serializer for {@link Animation} resources.
 */
public final class AnimationBinarySerializer implements BinaryResourceSerializer<Animation> {
    private static final int VERSION = 2;

    @Override
    public BinaryResourceType getType() {
        return BinaryResourceType.ANIMATION;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void write(BinaryDataWriter writer, Animation animation) throws IOException {
        if (animation == null) {
            writer.writeString("");
            writer.writeFloat(0f);
            writer.writeVarInt(Animation.LoopMode.ONCE.ordinal());
            writer.writeBoolean(false);
            writer.writeFloat(1f);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            return;
        }

        writer.writeString(safe(animation.getName()));
        writer.writeFloat(animation.getLength());
        writer.writeVarInt(animation.getLoopMode().ordinal());
        writer.writeBoolean(animation.isOverridePreviousAnimation());
        writer.writeFloat(animation.getSpeed());

        Map<String, Animation.BoneAnimation> bones = animation.getBoneAnimations();
        writer.writeVarInt(bones == null ? 0 : bones.size());
        if (bones != null) {
            for (Map.Entry<String, Animation.BoneAnimation> entry : bones.entrySet()) {
                writeBoneAnimation(writer, entry.getKey(), entry.getValue());
            }
        }

        writeEvents(writer, animation.getSoundEvents(), false);
        writeEvents(writer, animation.getParticleEvents(), true);
    }

    @Override
    public Animation read(BinaryDataReader reader) throws IOException {
        return read(reader, VERSION);
    }

    public Animation read(BinaryDataReader reader, int archiveVersion) throws IOException {
        String name = reader.readString();
        float length = reader.readFloat();
        int loopOrdinal = reader.readVarInt();
        boolean override = reader.readBoolean();
        float speed = reader.readFloat();

        Animation animation = new Animation(name == null || name.isEmpty() ? "animation" : name);
        animation.setLength(length);
        animation.setLoopMode(resolveLoopMode(loopOrdinal));
        animation.setOverridePreviousAnimation(override);
        animation.setSpeed(speed);

        int boneCount = Math.max(0, reader.readVarInt());
        for (int i = 0; i < boneCount; i++) {
            Animation.BoneAnimation boneAnimation = readBoneAnimation(reader);
            if (boneAnimation != null && boneAnimation.boneName != null) {
                animation.addBoneAnimation(boneAnimation.boneName, boneAnimation);
            }
        }

        readEvents(reader, animation.getSoundEvents(), Animation.Event.Type.SOUND, false);
        if (archiveVersion >= 2) {
            readEvents(reader, animation.getParticleEvents(), null, true);
        } else {
            readEvents(reader, animation.getParticleEvents(), Animation.Event.Type.PARTICLE, false);
            readEvents(reader, animation.getParticleEvents(), Animation.Event.Type.TRAIL, false);
        }
        return animation;
    }

    private void writeBoneAnimation(BinaryDataWriter writer, String boneName, Animation.BoneAnimation boneAnimation) throws IOException {
        writer.writeString(boneName == null ? "" : boneName);
        writeKeyFrames(writer, boneAnimation == null ? Collections.emptyList() : boneAnimation.positionFrames);
        writeKeyFrames(writer, boneAnimation == null ? Collections.emptyList() : boneAnimation.rotationFrames);
        writeKeyFrames(writer, boneAnimation == null ? Collections.emptyList() : boneAnimation.scaleFrames);
    }

    private Animation.BoneAnimation readBoneAnimation(BinaryDataReader reader) throws IOException {
        String boneName = reader.readString();
        Animation.BoneAnimation boneAnimation = new Animation.BoneAnimation(boneName == null ? "" : boneName);
        boneAnimation.positionFrames.addAll(readKeyFrames(reader));
        boneAnimation.rotationFrames.addAll(readKeyFrames(reader));
        boneAnimation.scaleFrames.addAll(readKeyFrames(reader));
        return boneAnimation;
    }

    private void writeKeyFrames(BinaryDataWriter writer, List<Animation.KeyFrame> frames) throws IOException {
        List<Animation.KeyFrame> list = frames == null ? Collections.emptyList() : frames;
        writer.writeVarInt(list.size());
        for (Animation.KeyFrame frame : list) {
            if (frame == null) {
                writer.writeFloat(0f);
                writer.writeVarInt(0);
                writer.writeString("linear");
                writer.writeFloatArray(null);
                writer.writeFloatArray(null);
                continue;
            }
            writer.writeFloat(frame.timestamp);
            writer.writeFloatArray(frame.value);
            writer.writeString(resolveInterpolationName(frame.interpolation));
            writer.writeFloatArray(frame.post);
            writer.writeFloatArray(frame.pre);
        }
    }

    private List<Animation.KeyFrame> readKeyFrames(BinaryDataReader reader) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        List<Animation.KeyFrame> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float timestamp = reader.readFloat();
            float[] value = reader.readFloatArray();
            String interpolationName = reader.readString();
            float[] post = reader.readFloatArray();
            float[] pre = reader.readFloatArray();
            Animation.KeyFrame frame = new Animation.KeyFrame(timestamp, value);
            frame.interpolation = InterpolationImpl.getInstance(interpolationName);
            frame.post = post.length == 0 ? null : post;
            frame.pre = pre.length == 0 ? null : pre;
            frames.add(frame);
        }
        return frames;
    }

    private void writeEvents(BinaryDataWriter writer,
                             List<Animation.Event> events,
                             boolean includeType) throws IOException {
        List<Animation.Event> list = events == null ? Collections.emptyList() : events;
        writer.writeVarInt(list.size());
        for (Animation.Event event : list) {
            writer.writeFloat(event == null ? 0f : event.getTimestamp());
            writer.writeString(event == null ? "" : safe(event.getEffect()));
            writer.writeString(event == null ? "" : safe(event.getLocator()));
            if (includeType) {
                int typeOrdinal = event == null || event.getType() == null
                    ? Animation.Event.Type.PARTICLE.ordinal()
                    : event.getType().ordinal();
                writer.writeVarInt(typeOrdinal);
            }
        }
    }

    private void readEvents(BinaryDataReader reader,
                            List<Animation.Event> target,
                            Animation.Event.Type defaultType,
                            boolean expectType) throws IOException {
        int count = Math.max(0, reader.readVarInt());
        for (int i = 0; i < count; i++) {
            float timestamp = reader.readFloat();
            String effect = reader.readString();
            String locator = reader.readString();
            Animation.Event.Type type = defaultType;
            if (expectType) {
                int ordinal = reader.readVarInt();
                type = resolveEventType(ordinal, Animation.Event.Type.PARTICLE);
            }
            if (type == null) {
                type = Animation.Event.Type.PARTICLE;
            }
            target.add(new Animation.Event(type, timestamp,
                effect == null ? "" : effect,
                locator == null ? "" : locator));
        }
    }

    private Animation.LoopMode resolveLoopMode(int ordinal) {
        Animation.LoopMode[] values = Animation.LoopMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return Animation.LoopMode.ONCE;
        }
        return values[ordinal];
    }

    private String resolveInterpolationName(Interpolation interpolation) {
        if (interpolation == null) {
            return "linear";
        }
        String name = interpolation.getName();
        return name == null ? "linear" : name;
    }

    private Animation.Event.Type resolveEventType(int ordinal, Animation.Event.Type fallback) {
        Animation.Event.Type[] types = Animation.Event.Type.values();
        if (ordinal < 0 || ordinal >= types.length) {
            return fallback;
        }
        return types[ordinal];
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
