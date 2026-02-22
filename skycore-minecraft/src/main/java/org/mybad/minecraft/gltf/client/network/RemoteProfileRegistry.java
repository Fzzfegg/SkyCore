package org.mybad.minecraft.gltf.client.network;

import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.GltfProfile;

import org.mybad.skycoreproto.SkyCoreProto;

import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public final class RemoteProfileRegistry {

    private static final Map<String, GltfProfile> PROFILES = new ConcurrentHashMap<>();

    private RemoteProfileRegistry() {
    }

    public static void registerProfile(String profileId, GltfProfile config) {
        if (profileId == null || config == null) {
            return;
        }
        PROFILES.put(profileId, config);
    }

    public static void clear() {
        PROFILES.clear();
    }

    @Nullable
    public static GltfProfile getProfile(String profileId) {
        if (profileId == null) {
            return null;
        }
        return PROFILES.get(profileId);
    }

    public static void handleProfileDefinition(SkyCoreProto.GltfProfile proto) {
        if (proto == null || proto.getProfileId().isEmpty()) {
            return;
        }
        if (proto.getModelPath().isEmpty()) {
            GltfLog.LOGGER.warn("Ignored GLTF profile {} due to empty model path", proto.getProfileId());
            return;
        }
        java.util.HashMap<String, GltfProfile.AnimationClip> clips = new java.util.HashMap<>();
        if (proto.getAnimationsCount() > 0) {
            for (SkyCoreProto.GltfAnimationClip clip : proto.getAnimationsList()) {
                if (clip == null || clip.getName().isEmpty()) {
                    continue;
                }
                Double blend = clip.getBlendDuration() > 0 ? clip.getBlendDuration() : null;
                Boolean loopFlag = clip.getLoop() ? Boolean.TRUE : null;
                Boolean holdLast = clip.getHoldLastFrame() ? Boolean.TRUE : null;
                GltfProfile.AnimationClip anim = new GltfProfile.AnimationClip(
                    clip.getStartTime(),
                    clip.getEndTime(),
                    clip.getSpeed() > 0 ? clip.getSpeed() : 1.0,
                    blend,
                    loopFlag,
                    holdLast
                );
                clips.put(clip.getName(), anim);
            }
        }
        String hash = proto.getHash();
        if (hash == null || hash.isEmpty()) {
            hash = computeHashFallback(proto);
        }
        long version = proto.getVersion();
        GltfProfile profile = new GltfProfile(
            proto.getProfileId(),
            proto.getModelPath(),
            proto.getTexturePath().isEmpty() ? null : proto.getTexturePath(),
            proto.getModelScale() > 0 ? proto.getModelScale() : 1.0f,
            proto.getFps() > 0 ? proto.getFps() : 24,
            proto.getBlendDuration() > 0 ? proto.getBlendDuration() : 0.2,
            hash,
            version,
            clips
        );
        registerProfile(proto.getProfileId(), profile);
        logProfile(proto);
        org.mybad.minecraft.resource.preload.PreloadManager manager = SkyCoreMod.getPreloadManager();
        if (manager != null) {
            manager.enqueueGltfProfile(profile);
        }
    }

    private static void logProfile(SkyCoreProto.GltfProfile proto) {
        StringBuilder builder = new StringBuilder("[SkyCore][GLTF] Profile ")
            .append(proto.getProfileId())
            .append(": model=")
            .append(proto.getModelPath().isEmpty() ? "<empty>" : proto.getModelPath());
        if (!proto.getTexturePath().isEmpty()) {
            builder.append(" tex=").append(proto.getTexturePath());
        }
        if (proto.getModelScale() > 0) {
            builder.append(" scale=").append(proto.getModelScale());
        }
        if (proto.getFps() > 0) {
            builder.append(" fps=").append(proto.getFps());
        }
        if (proto.getBlendDuration() > 0) {
            builder.append(" blend=").append(proto.getBlendDuration());
        }
        if (proto.getAnimationsCount() > 0) {
            builder.append(" animations=").append(proto.getAnimationsCount());
        }
        if (proto.getVersion() > 0) {
            builder.append(" ver=").append(proto.getVersion());
        }
        if (!proto.getHash().isEmpty()) {
            builder.append(" hash=").append(proto.getHash());
        }
        GltfLog.LOGGER.info(builder.toString());
    }

    // Player-specific GLTF assignments have been removed for simplified entity-only use cases.

    private static String computeHashFallback(SkyCoreProto.GltfProfile proto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            SkyCoreProto.GltfProfile sanitized = proto.toBuilder()
                .clearHash()
                .clearVersion()
                .build();
            byte[] bytes = digest.digest(sanitized.toByteArray());
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
