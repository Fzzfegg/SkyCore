package org.mybad.minecraft.gltf.client.network;

import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.gltf.client.CustomPlayerConfig;
import org.mybad.minecraft.gltf.client.CustomPlayerManager;

import org.mybad.skycoreproto.SkyCoreProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public final class RemoteProfileRegistry {

    private static final Map<String, CustomPlayerConfig> PROFILES = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> PENDING_ASSIGNMENTS = new ConcurrentHashMap<>();

    private RemoteProfileRegistry() {
    }

    public static void registerProfile(String profileId, CustomPlayerConfig config) {
        if (profileId == null || config == null) {
            return;
        }
        config.name = profileId;
        PROFILES.put(profileId, config);
        CustomPlayerManager.registerRemoteConfig(profileId, config);
        List<String> pending = PENDING_ASSIGNMENTS.remove(profileId);
        if (pending != null) {
            for (String playerName : pending) {
                CustomPlayerManager.setPlayerConfiguration(playerName, profileId);
            }
        }
    }

    public static void assignProfile(String playerName, String profileId) {
        if (playerName == null || profileId == null) {
            return;
        }
        CustomPlayerConfig profile = PROFILES.get(profileId);
        if (profile != null) {
            CustomPlayerManager.registerRemoteConfig(profileId, profile);
            CustomPlayerManager.setPlayerConfiguration(playerName, profileId);
        } else {
            PENDING_ASSIGNMENTS.computeIfAbsent(profileId, key -> new ArrayList<>()).add(playerName);
        }
    }

    public static void clear() {
        PROFILES.clear();
        PENDING_ASSIGNMENTS.clear();
        CustomPlayerManager.clearRemoteConfigs();
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
        // animations field requires updated proto; guarded by generated accessors
        try {
            java.util.List<?> clipsList = (java.util.List<?>) proto.getClass().getMethod("getAnimationsList").invoke(proto);
            if (clipsList != null && !clipsList.isEmpty()) {
                java.util.HashMap<String, CustomPlayerConfig.AnimationConfig> clips = new java.util.HashMap<>();
                for (Object clipObj : clipsList) {
                    if (clipObj == null) {
                        continue;
                    }
                    Class<?> clipClass = clipObj.getClass();
                    String name = (String) clipClass.getMethod("getName").invoke(clipObj);
                    if (name == null || name.isEmpty()) {
                        continue;
                    }
                    CustomPlayerConfig.AnimationConfig anim = new CustomPlayerConfig.AnimationConfig();
                    anim.startTime = ((Number) clipClass.getMethod("getStartTime").invoke(clipObj)).doubleValue();
                    anim.endTime = ((Number) clipClass.getMethod("getEndTime").invoke(clipObj)).doubleValue();
                    double speed = ((Number) clipClass.getMethod("getSpeed").invoke(clipObj)).doubleValue();
                    if (speed > 0) {
                        anim.speed = speed;
                    }
                    double blend = ((Number) clipClass.getMethod("getBlendDuration").invoke(clipObj)).doubleValue();
                    if (blend > 0) {
                        anim.blendDuration = blend;
                    }
                    boolean loop = (Boolean) clipClass.getMethod("getLoop").invoke(clipObj);
                    if (loop) {
                        anim.loop = Boolean.TRUE;
                    }
                    boolean holdLast = (Boolean) clipClass.getMethod("getHoldLastFrame").invoke(clipObj);
                    if (holdLast) {
                        anim.holdLastFrame = Boolean.TRUE;
                    }
                    clips.put(name, anim);
                }
                if (!clips.isEmpty()) {
                    config.animations = clips;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        registerProfile(proto.getProfileId(), config);
        org.mybad.minecraft.resource.preload.PreloadManager manager = SkyCoreMod.getPreloadManager();
        if (manager != null) {
            manager.enqueueGltfProfile(proto.getProfileId(), config);
        }
    }

    public static void handleProfileAssignment(SkyCoreProto.GltfProfileAssignment proto) {
        if (proto == null || proto.getPlayerName().isEmpty()) {
            return;
        }
        if (proto.getClear()) {
            CustomPlayerManager.clearPlayerConfiguration(proto.getPlayerName());
            return;
        }
        assignProfile(proto.getPlayerName(), proto.getProfileId());
    }
}
