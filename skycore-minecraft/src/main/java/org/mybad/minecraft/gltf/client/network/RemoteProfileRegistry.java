package org.mybad.minecraft.gltf.client.network;

import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.gltf.client.CustomPlayerConfig;

import org.mybad.skycoreproto.SkyCoreProto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public final class RemoteProfileRegistry {

    private static final Map<String, CustomPlayerConfig> PROFILES = new ConcurrentHashMap<>();

    private RemoteProfileRegistry() {
    }

    public static void registerProfile(String profileId, CustomPlayerConfig config) {
        if (profileId == null || config == null) {
            return;
        }
        config.name = profileId;
        PROFILES.put(profileId, config);
    }

    public static void clear() {
        PROFILES.clear();
    }

    @Nullable
    public static CustomPlayerConfig getProfile(String profileId) {
        if (profileId == null) {
            return null;
        }
        return PROFILES.get(profileId);
    }

    public static void handleProfileDefinition(SkyCoreProto.GltfProfile proto) {
        if (proto == null || proto.getProfileId().isEmpty()) {
            return;
        }
        CustomPlayerConfig config = new CustomPlayerConfig();
        config.name = proto.getProfileId();
        if (!proto.getModelPath().isEmpty()) {
            config.modelPath = proto.getModelPath();
        }
        if (!proto.getTexturePath().isEmpty()) {
            config.texturePath = proto.getTexturePath();
        }
        if (proto.getModelScale() > 0) {
            config.modelScale = proto.getModelScale();
        }
        if (proto.getFps() > 0) {
            config.fps = proto.getFps();
        }
        if (proto.getBlendDuration() > 0) {
            config.blendDuration = proto.getBlendDuration();
        }
        if (proto.getAnimationsCount() > 0) {
            java.util.HashMap<String, CustomPlayerConfig.AnimationConfig> clips = new java.util.HashMap<>();
            for (SkyCoreProto.GltfAnimationClip clip : proto.getAnimationsList()) {
                if (clip == null || clip.getName().isEmpty()) {
                    continue;
                }
                CustomPlayerConfig.AnimationConfig anim = new CustomPlayerConfig.AnimationConfig();
                anim.startTime = clip.getStartTime();
                anim.endTime = clip.getEndTime();
                if (clip.getSpeed() > 0) {
                    anim.speed = clip.getSpeed();
                }
                if (clip.getBlendDuration() > 0) {
                    anim.blendDuration = clip.getBlendDuration();
                }
                if (clip.getLoop()) {
                    anim.loop = Boolean.TRUE;
                }
                if (clip.getHoldLastFrame()) {
                    anim.holdLastFrame = Boolean.TRUE;
                }
                clips.put(clip.getName(), anim);
            }
            if (!clips.isEmpty()) {
                config.animations = clips;
            }
        }
        registerProfile(proto.getProfileId(), config);
        org.mybad.minecraft.resource.preload.PreloadManager manager = SkyCoreMod.getPreloadManager();
        if (manager != null) {
            manager.enqueueGltfProfile(proto.getProfileId(), config);
        }
    }

    // Player-specific GLTF assignments have been removed for simplified entity-only use cases.
}
