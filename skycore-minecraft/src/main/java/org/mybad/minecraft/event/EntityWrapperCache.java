package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.resource.ResourceLoader;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityWrapperCache {
    private final ResourceLoader resourceLoader;
    private final Map<Integer, WrapperEntry> cache;

    EntityWrapperCache(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.cache = new ConcurrentHashMap<>();
    }

    Collection<WrapperEntry> entries() {
        return cache.values();
    }

    WrapperEntry getOrCreate(EntityLivingBase entity, String entityName, EntityModelMapping mapping) {
        int entityId = entity.getEntityId();
        long tick = entity.world != null ? entity.world.getTotalWorldTime() : 0L;
        WrapperEntry entry = cache.get(entityId);
        if (entry != null) {
            if (!entity.getUniqueID().equals(entry.entityUuid) || !entityName.equals(entry.mappingName)) {
                entry.wrapper.dispose();
                cache.remove(entityId);
            } else {
                entry.lastSeenTick = tick;
                return entry;
            }
        }

        Model model = resourceLoader.loadModel(mapping.getModel());
        if (model == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型: {} for entity: {}", mapping.getModel(), entityName);
            return null;
        }

        Animation animation = null;
        if (mapping.getAnimation() != null && !mapping.getAnimation().isEmpty()) {
            animation = resourceLoader.loadAnimation(mapping.getAnimation());
        }

        ResourceLocation texture = resourceLoader.getTextureLocation(mapping.getTexture());
        ResourceLocation emissiveTexture = null;
        if (mapping.getEmissive() != null && !mapping.getEmissive().isEmpty()) {
            emissiveTexture = resourceLoader.getTextureLocation(mapping.getEmissive());
        }

        BedrockModelHandle wrapper = BedrockModelHandle.create(
            model,
            animation,
            texture,
            emissiveTexture,
            mapping.isEnableCull(),
            mapping.getModel(),
            resourceLoader.getGeometryCache()
        );
        wrapper.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        wrapper.setEmissiveStrength(mapping.getEmissiveStrength());
        wrapper.setModelScale(mapping.getModelScale());

        EntityAnimationController controller = buildController(mapping);
        WrapperEntry created = new WrapperEntry(wrapper, controller, entity.getUniqueID(), entityName, tick);
        cache.put(entityId, created);

        SkyCoreMod.LOGGER.info("[SkyCore] 为实体 '{}' 创建模型包装器", entityName);
        return created;
    }

    void clear() {
        for (WrapperEntry entry : cache.values()) {
            entry.wrapper.dispose();
        }
        cache.clear();
    }

    void invalidateByName(String entityName) {
        for (java.util.Iterator<Map.Entry<Integer, WrapperEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WrapperEntry> entry = it.next();
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
        for (java.util.Iterator<Map.Entry<Integer, WrapperEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WrapperEntry> entry = it.next();
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
        Map<String, Animation> actions = resourceLoader.loadAnimationSet(basePath);
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        return new EntityAnimationController(actions);
    }
}
