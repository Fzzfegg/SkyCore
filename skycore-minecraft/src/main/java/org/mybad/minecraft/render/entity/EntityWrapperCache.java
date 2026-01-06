package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.resource.ResourceCacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityWrapperCache {
    private final ResourceCacheManager cacheManager;
    private final Map<Integer, EntityWrapperEntry> cache;

    EntityWrapperCache(ResourceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.cache = new ConcurrentHashMap<>();
    }

    Collection<EntityWrapperEntry> entries() {
        return cache.values();
    }

    EntityWrapperEntry getOrCreate(EntityLivingBase entity, String entityName, EntityModelMapping mapping) {
        int entityId = entity.getEntityId();
        long tick = entity.world != null ? entity.world.getTotalWorldTime() : 0L;
        EntityWrapperEntry entry = cache.get(entityId);
        if (entry != null) {
            if (!entity.getUniqueID().equals(entry.entityUuid) || !entityName.equals(entry.mappingName)) {
                entry.wrapper.dispose();
                cache.remove(entityId);
            } else {
                entry.lastSeenTick = tick;
                return entry;
            }
        }

        BedrockModelHandle wrapper = ModelHandleFactory.create(cacheManager, mapping);
        if (wrapper == null) {
            return null;
        }
        SkyCoreConfig.RenderConfig renderConfig = SkyCoreConfig.getInstance().getRenderConfig();
        wrapper.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        wrapper.setEmissiveStrength(mapping.getEmissiveStrength());
        wrapper.setBloomStrength(renderConfig.bloomStrength);
        wrapper.setBloomRadius(renderConfig.bloomRadius);
        wrapper.setBloomDownsample(renderConfig.bloomDownsample);
        wrapper.setBloomThreshold(renderConfig.bloomThreshold);
        wrapper.setModelScale(mapping.getModelScale());
        wrapper.setRenderHurtTint(mapping.isRenderHurtTint());
        wrapper.setHurtTint(mapping.getHurtTint());

        EntityAnimationController controller = buildController(mapping);
        EntityWrapperEntry created = new EntityWrapperEntry(wrapper, controller, entity.getUniqueID(), entityName, tick);
        cache.put(entityId, created);

        SkyCoreMod.LOGGER.info("[SkyCore] 为实体 '{}' 创建模型包装器", entityName);
        return created;
    }

    void clear() {
        for (EntityWrapperEntry entry : cache.values()) {
            entry.wrapper.dispose();
        }
        cache.clear();
    }

    void invalidateByName(String entityName) {
        for (java.util.Iterator<Map.Entry<Integer, EntityWrapperEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, EntityWrapperEntry> entry = it.next();
            if (entityName.equals(entry.getValue().mappingName)) {
                entry.getValue().wrapper.dispose();
                it.remove();
            }
        }
    }

    void cleanupDead() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        for (java.util.Iterator<Map.Entry<Integer, EntityWrapperEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, EntityWrapperEntry> entry = it.next();
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (entity == null || entity.isDead) {
                entry.getValue().wrapper.dispose();
                it.remove();
            }
        }
    }

    private EntityAnimationController buildController(EntityModelMapping mapping) {
        String basePath = mapping.getAnimation();
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }
        Map<String, Animation> actions = cacheManager.loadAnimationSet(basePath);
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        return new EntityAnimationController(actions);
    }
}
