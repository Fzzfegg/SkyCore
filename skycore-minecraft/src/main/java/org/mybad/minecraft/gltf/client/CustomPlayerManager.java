package org.mybad.minecraft.gltf.client;

import java.util.HashMap;
import java.util.Map;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.core.data.DataMaterial;
import org.mybad.minecraft.gltf.core.data.GltfDataModel;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class CustomPlayerManager {
    private static final HashMap<String, CachedModel> modelCache = new HashMap<>();
    private static final HashMap<String, CustomPlayerConfig> remotePlayerConfigs = new HashMap<>();
    private static final HashMap<String, CustomPlayerInstance> playerInstances = new HashMap<>();
    private static final HashMap<String, GltfRenderModel> profileModelCache = new HashMap<>();
    private static final HashMap<String, LogLimiter> missingModelLogLimiter = new HashMap<>();
    private static final HashMap<String, LogLimiter> missingTextureLogLimiter = new HashMap<>();
    private static final int LIMITER_MAX_SIZE = 4096;
    private static final long LIMITER_STALE_MS = 10 * 60_000L; // 10 minutes

    private CustomPlayerManager() {
    }

    public static CustomPlayerInstance getPlayerInstance(String playerName) {
        return playerInstances.computeIfAbsent(playerName, name -> new CustomPlayerInstance());
    }

    /**
     * Always load a fresh render model (no cache), for configs that need isolated materials.
     */
    @Nullable
    public static GltfRenderModel loadModelFresh(String modelPath) {
        try {
            ResourceLocation location = new ResourceLocation(modelPath);
            GltfDataModel dataModel = GltfDataModel.load(location);
            if (dataModel.loaded) {
                GltfRenderModel model = new GltfRenderModel(dataModel);
                model.setDebugSourceId(modelPath + " (fresh)");
                return model;
            }
        } catch (Exception e) {
            if (shouldLogMissingModel(modelPath)) {
                GltfLog.LOGGER.error("Error loading model (fresh): " + modelPath, e);
            }
        }
        return null;
    }

    public static void setPlayerConfiguration(String playerName, String configName) {
        CustomPlayerConfig config = resolveConfig(configName);
        CustomPlayerInstance instance = getPlayerInstance(playerName);
        if (config != null) {
            instance.bindConfiguration(config);
            GltfLog.LOGGER.info("Set player " + playerName + " to use remote profile: " + configName);
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("Player {} bound to profile {} (modelPath={}, activeModels={})",
                    playerName, configName, config.modelPath, GltfRenderModel.getActiveInstanceCount());
            }
        } else {
            instance.unbindModel();
            GltfLog.LOGGER.warn("Remote profile not found: " + configName);
        }
    }

    public static void clearPlayerConfiguration(String playerName) {
        if (playerName == null) {
            return;
        }
        CustomPlayerInstance instance = playerInstances.get(playerName);
        if (instance != null) {
            instance.unbindModel();
        }
    }

    public static GltfRenderModel getOrLoadModel(String modelPath) {
        CachedModel entry = modelCache.computeIfAbsent(modelPath, key -> new CachedModel());

        if (entry.renderModel != null && entry.renderModel.geoModel.loaded) {
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("[ModelCache] Reusing cached model {} (instance={}, active={})",
                    modelPath, entry.renderModel.getInstanceId(), GltfRenderModel.getActiveInstanceCount());
            }
            return entry.renderModel;
        }

        if (entry.renderModel != null) {
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("[ModelCache] Reloading model {} - previous instance {} not fully loaded",
                    modelPath, entry.renderModel.getInstanceId());
            }
            scheduleCleanup(entry.renderModel);
            entry.renderModel = null;
        }

        try {
            ResourceLocation location = new ResourceLocation(modelPath);
            GltfDataModel dataModel = GltfDataModel.load(location);
            if (dataModel.loaded) {
                entry.renderModel = new GltfRenderModel(dataModel);
                entry.renderModel.setDebugSourceId(modelPath);
                logCacheEvent("Loaded model " + modelPath + " (instance " + entry.renderModel.getInstanceId() + ")");
                return entry.renderModel;
            }
        } catch (Exception e) {
            if (shouldLogMissingModel(modelPath)) {
                GltfLog.LOGGER.error("Error loading model: " + modelPath, e);
            }
        }

        modelCache.remove(modelPath);
        return null;
    }

    public static boolean renderCustomPlayer(EntityPlayer player, double x, double y, double z,
                                             float entityYaw, float partialTicks) {
        try {
            CustomPlayerInstance instance = getPlayerInstance(player.getName());
            return instance.render(player, x, y, z, entityYaw, partialTicks);
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering custom player: " + player.getName(), e);
            return false;
        }
    }

    @Nullable
    public static Vec3d getBoneWorldPosition(EntityPlayer player, String boneName, float partialTicks) {
        if (player == null || boneName == null || boneName.isEmpty()) {
            return null;
        }
        CustomPlayerInstance instance = playerInstances.get(player.getName());
        if (instance == null) {
            return null;
        }
        return instance.sampleBoneWorldPosition(player, boneName, partialTicks);
    }
    public static void cleanup() {
        modelCache.values().forEach(entry -> {
            if (entry.renderModel != null) {
                logCacheEvent("Manual cleanup requested for " + entry.renderModel.getDebugSourceId()
                    + " (instance " + entry.renderModel.getInstanceId() + ")");
                entry.renderModel.cleanup();
            }
        });
        modelCache.clear();
        playerInstances.values().forEach(instance -> {
            try {
                instance.unbindModel();
            } catch (Exception ignored) {
            }
        });
        playerInstances.clear();
        remotePlayerConfigs.clear();
    }

    public static void registerRemoteConfig(String profileId, CustomPlayerConfig config) {
        if (profileId == null || config == null) {
            return;
        }
        config.name = profileId;
        CustomPlayerConfig previous = remotePlayerConfigs.put(profileId, config);
        if (previous != null && previous.modelPath != null) {
            invalidateModel(previous.modelPath);
        }
        onProfileAvailable(profileId);
    }

    public static void clearRemoteConfigs() {
        remotePlayerConfigs.clear();
        profileModelCache.values().forEach(m -> {
            try { m.cleanup(); } catch (Exception ignored) {}
        });
        profileModelCache.clear();
    }

    public static void reloadResources() {
        modelCache.values().forEach(entry -> {
            if (entry.renderModel != null) {
                entry.renderModel.cleanup();
                entry.renderModel = null;
            }
        });
        modelCache.entrySet().removeIf(e -> e.getValue().renderModel == null);
        playerInstances.forEach((name, instance) -> {
            CustomPlayerConfig config = instance.getConfig();
            if (config != null) {
                instance.bindConfiguration(config);
            } else {
                instance.unbindModel();
            }
        });
    }

    private static void invalidateModel(String modelPath) {
        if (modelPath == null) {
            return;
        }
        CachedModel entry = modelCache.remove(modelPath);
        if (entry != null && entry.renderModel != null) {
            logCacheEvent("Invalidating cached model " + modelPath + " (instance " + entry.renderModel.getInstanceId() + ")");
            entry.renderModel.cleanup();
        }
        // also remove any profile models referencing this path
        profileModelCache.entrySet().removeIf(e -> {
            GltfRenderModel m = e.getValue();
            if (m != null && modelPath.equals(m.getDebugSourceId())) {
                m.cleanup();
                return true;
            }
            return false;
        });
    }

    /**
     * Per profile isolated render model (materials not shared). Geometry is still loaded fresh.
     */
    @Nullable
    public static GltfRenderModel getOrCreateProfileModel(CustomPlayerConfig config) {
        if (config == null || config.name == null || config.modelPath == null) {
            return null;
        }
        GltfRenderModel cached = profileModelCache.get(config.name);
        if (cached != null) {
            return cached;
        }
        GltfRenderModel fresh = loadModelFresh(config.modelPath);
        if (fresh != null) {
            profileModelCache.put(config.name, fresh);
        }
        return fresh;
    }

    public static String[] getRegisteredProfileIds() {
        return remotePlayerConfigs.keySet().toArray(new String[0]);
    }

    private static CustomPlayerConfig resolveConfig(String configName) {
        if (configName == null) {
            return null;
        }
        return remotePlayerConfigs.get(configName);
    }

    private static void scheduleCleanup(GltfRenderModel model) {
        if (model == null) {
            return;
        }
        logCacheEvent("Scheduling cleanup for model instance " + model.getInstanceId()
            + " (source=" + model.getDebugSourceId() + ")");
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.addScheduledTask(model::cleanup);
        } else {
            model.cleanup();
        }
    }


    private static final class CachedModel {
        private GltfRenderModel renderModel;
    }

    private static void logCacheEvent(String message) {
        if (GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("[ModelCache] {} (cacheSize={}, activeModels={})",
                message, modelCache.size(), GltfRenderModel.getActiveInstanceCount());
        }
    }

    /**
     * Limit how often we spam logs for missing / broken models.
     * At most once every 10 seconds per model, and no more than 10 times total.
     */
    public static boolean shouldLogMissingModel(String modelPath) {
        if (modelPath == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        LogLimiter limiter = missingModelLogLimiter.computeIfAbsent(modelPath, k -> new LogLimiter());
        cleanupLimiters(missingModelLogLimiter, now);
        if (limiter.count >= 10) {
            return false;
        }
        if (now - limiter.lastLogMs < 10_000L) {
            return false;
        }
        limiter.lastLogMs = now;
        limiter.count++;
        return true;
    }

    /**
     * Limit emissive/aux texture missing logs to once every 10s per texture, max 10 times.
     */
    public static boolean shouldLogMissingTexture(String texturePath) {
        if (texturePath == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        LogLimiter limiter = missingTextureLogLimiter.computeIfAbsent(texturePath, k -> new LogLimiter());
        cleanupLimiters(missingTextureLogLimiter, now);
        if (limiter.count >= 10) {
            return false;
        }
        if (now - limiter.lastLogMs < 10_000L) {
            return false;
        }
        limiter.lastLogMs = now;
        limiter.count++;
        return true;
    }

    private static void cleanupLimiters(HashMap<String, LogLimiter> map, long now) {
        if (map.size() <= LIMITER_MAX_SIZE / 2) {
            return;
        }
        map.entrySet().removeIf(e -> (e.getValue().count >= 10 && now - e.getValue().lastLogMs > 60_000L)
            || (now - e.getValue().lastLogMs > LIMITER_STALE_MS)
            || map.size() > LIMITER_MAX_SIZE);
    }

    private static final class LogLimiter {
        private long lastLogMs = 0L;
        private int count = 0;
    }

    public static void onProfileAvailable(String profileId) {
        if (profileId == null) {
            return;
        }
        playerInstances.values().forEach(instance -> {
            if (instance != null) {
                instance.onAttachmentProfileAvailable(profileId);
            }
        });
    }

    public static void applyOverlayPulseOverride(String playerName, String materialName, String overlayId,
                                                 DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs) {
        if (playerName == null) {
            return;
        }
        CustomPlayerInstance instance = getPlayerInstance(playerName);
        if (instance != null) {
            instance.applyOverlayPulseOverride(materialName, overlayId, pulse, durationMs);
        }
    }

    public static void applyOverlayColorPulseOverride(String playerName, String materialName, String overlayId,
                                                      DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs) {
        if (playerName == null) {
            return;
        }
        CustomPlayerInstance instance = getPlayerInstance(playerName);
        if (instance != null) {
            instance.applyOverlayColorPulseOverride(materialName, overlayId, pulse, durationMs);
        }
    }
}
