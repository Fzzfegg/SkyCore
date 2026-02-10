package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.resource.ResourceCacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
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
        if (entity.isDead) {
            return null;
        }

        BedrockModelHandle wrapper = ModelHandleFactory.create(cacheManager, mapping);
        if (wrapper == null) {
            return null;
        }
        wrapper.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        wrapper.setEmissiveStrength(mapping.getEmissiveStrength());
        wrapper.setBloomStrength(mapping.getBloomStrength());
        wrapper.setBloomColor(mapping.getBloomColor());
        wrapper.setBloomPasses(mapping.getBloomPasses());
        wrapper.setBloomScaleStep(mapping.getBloomScaleStep());
        wrapper.setBloomDownscale(mapping.getBloomDownscale());
        wrapper.setBloomOffset(mapping.getBloomOffset());
        SkyCoreMod.LOGGER.info("[SkyCore] bloom params for '{}' -> passes={}, scaleStep={}, downscale={}",
            mapping.getName(), mapping.getBloomPasses(), mapping.getBloomScaleStep(), mapping.getBloomDownscale());
        wrapper.setModelScale(mapping.getModelScale());
        wrapper.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());
        wrapper.setRenderHurtTint(mapping.isRenderHurtTint());
        wrapper.setHurtTint(mapping.getHurtTint());

        EntityAnimationController controller = buildController(mapping);
        EntityWrapperEntry created = new EntityWrapperEntry(wrapper, controller, entity.getUniqueID(), entityName, tick, mapping);
        cache.put(entityId, created);

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

    void cleanupDead(LingeringEntityManager lingeringManager) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        for (java.util.Iterator<Map.Entry<Integer, EntityWrapperEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, EntityWrapperEntry> entry = it.next();
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (entity == null || entity.isDead) {
                EntityWrapperEntry wrapperEntry = entry.getValue();
                boolean adopted = lingeringManager != null && lingeringManager.adopt(entity, wrapperEntry);
                if (!adopted) {
                    wrapperEntry.wrapper.dispose();
                }
                it.remove();
            }
        }
    }

    String findMappingNameByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (EntityWrapperEntry entry : cache.values()) {
            if (uuid.equals(entry.entityUuid)) {
                return entry.mappingName;
            }
        }
        return null;
    }

    void forEach(java.util.function.BiConsumer<EntityLivingBase, EntityWrapperEntry> consumer) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        for (Map.Entry<Integer, EntityWrapperEntry> entry : cache.entrySet()) {
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            consumer.accept((EntityLivingBase) entity, entry.getValue());
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
