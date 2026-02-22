package org.mybad.minecraft.gltf.client;

import java.util.HashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.core.data.GltfDataModel;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;

/**
 * Legacy name retained for compatibility – now only provides model caching helpers
 * for non-player GLTF entities and decorations.
 */
public final class CustomPlayerManager {

    private static final HashMap<String, CachedModel> modelCache = new HashMap<>();
    private static final HashMap<String, LogLimiter> missingModelLogLimiter = new HashMap<>();
    private static final HashMap<String, LogLimiter> missingTextureLogLimiter = new HashMap<>();
    private static final int LIMITER_MAX_SIZE = 4096;
    private static final long LIMITER_STALE_MS = 10 * 60_000L; // 10 minutes

    private CustomPlayerManager() {
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

    @Nullable
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
}
