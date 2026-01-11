package org.mybad.minecraft.resource.preload;

import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.skycoreproto.SkyCoreProto;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PreloadManager {

    private final ResourceCacheManager cacheManager;
    private final ConcurrentLinkedQueue<PreloadTask> queue = new ConcurrentLinkedQueue<>();
    private final Set<String> scheduledKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> warmedResources = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SkyCore-Preload");
        thread.setDaemon(true);
        return thread;
    });

    public PreloadManager(ResourceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void enqueue(SkyCoreProto.PreloadHint hint) {
        if (hint == null) {
            return;
        }
        String group = hint.getGroup();
        hint.getMappingNamesList().forEach(name -> enqueue(TaskType.MAPPING, name, group));
        hint.getTexturesList().forEach(texture -> enqueue(TaskType.TEXTURE, texture, group));
        hint.getParticlesList().forEach(particle -> enqueue(TaskType.PARTICLE, particle, group));
        hint.getSoundsList().forEach(sound -> enqueue(TaskType.SOUND, sound, group));
        drain();
    }

    public void clear() {
        queue.clear();
        scheduledKeys.clear();
    }

    public void shutdown() {
        executor.shutdownNow();
        clear();
    }

    private void enqueue(TaskType type, String identifier, String group) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }
        String normalized = identifier.trim();
        String key = makeKey(type, normalized);
        if (!scheduledKeys.add(key)) {
            return;
        }
        queue.add(new PreloadTask(type, normalized, group));
    }

    private void drain() {
        if (queue.isEmpty()) {
            return;
        }
        if (!workerRunning.compareAndSet(false, true)) {
            return;
        }
        executor.submit(() -> {
            try {
                PreloadTask task;
                while ((task = queue.poll()) != null) {
                    try {
                        processTask(task);
                    } catch (Exception ex) {
                        SkyCoreMod.LOGGER.warn("[SkyCore] 预热任务 {} 失败: {}", task.identifier, ex.getMessage());
                    } finally {
                        scheduledKeys.remove(makeKey(task.type, task.identifier));
                    }
                }
            } finally {
                workerRunning.set(false);
                if (!queue.isEmpty()) {
                    drain();
                }
            }
        });
    }

    private void processTask(PreloadTask task) {
        switch (task.type) {
            case MAPPING:
                preloadMapping(task.identifier);
                break;
            case PARTICLE:
                preloadParticle(task.identifier);
                break;
            case TEXTURE:
                warmBinary(task.identifier, "texture");
                break;
            case SOUND:
                warmBinary(task.identifier, "sound");
                break;
        }
    }

    private void preloadMapping(String mappingName) {
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 预热失败：找不到映射 {}", mappingName);
            return;
        }
        long start = System.currentTimeMillis();
        int count = 0;
        if (!isBlank(mapping.getModel()) && cacheManager.loadModel(mapping.getModel()) != null) {
            count++;
        }
        if (!isBlank(mapping.getAnimation())) {
            try {
                Map<String, org.mybad.core.animation.Animation> set = cacheManager.loadAnimationSet(mapping.getAnimation());
                if (set != null && !set.isEmpty()) {
                    count++;
                }
            } catch (Exception ex) {
                SkyCoreMod.LOGGER.warn("[SkyCore] 预热动画失败 {}: {}", mapping.getAnimation(), ex.getMessage());
            }
        }
        warmBinary(mapping.getTexture(), "texture");
        warmBinary(mapping.getEmissive(), "texture");
        warmBinary(mapping.getBloom(), "texture");
        warmBinary(mapping.getBlendTexture(), "texture");
        long cost = System.currentTimeMillis() - start;
        SkyCoreMod.LOGGER.info("[SkyCore] 预热映射 {} 完成 ({} 资源, {} ms)", mappingName, count, cost);
    }

    private void preloadParticle(String path) {
        if (isBlank(path)) {
            return;
        }
        long start = System.currentTimeMillis();
        if (cacheManager.loadParticle(path) != null) {
            SkyCoreMod.LOGGER.info("[SkyCore] 预热粒子 {} 完成 ({} ms)", path, System.currentTimeMillis() - start);
        } else {
            SkyCoreMod.LOGGER.warn("[SkyCore] 预热粒子失败：{}", path);
        }
    }

    private void warmBinary(String path, String type) {
        if (isBlank(path)) {
            return;
        }
        String normalized = path.trim();
        String key = type + ":" + normalized.toLowerCase(Locale.ROOT);
        if (!warmedResources.add(key)) {
            return;
        }
        boolean ok = cacheManager.getResolver().prefetchBinary(normalized);
        if (!ok) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 预热 {} 资源失败：{}", type, normalized);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String makeKey(TaskType type, String identifier) {
        return type.name() + ":" + identifier.toLowerCase(Locale.ROOT);
    }

    private enum TaskType {
        MAPPING,
        TEXTURE,
        PARTICLE,
        SOUND
    }

    private static final class PreloadTask {
        final TaskType type;
        final String identifier;
        final String group;

        PreloadTask(TaskType type, String identifier, String group) {
            this.type = Objects.requireNonNull(type);
            this.identifier = identifier;
            this.group = group;
        }
    }
}
