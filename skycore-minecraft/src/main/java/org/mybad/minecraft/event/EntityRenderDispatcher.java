package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelWrapper;
import org.mybad.minecraft.resource.ResourceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
final class EntityRenderDispatcher {
    private final ResourceLoader resourceLoader;
    private final Map<Integer, WrapperEntry> modelWrapperCache;
    private final Map<String, Animation> forcedAnimations;
    private final AnimationEventDispatcher eventDispatcher;

    EntityRenderDispatcher(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.modelWrapperCache = new ConcurrentHashMap<>();
        this.forcedAnimations = new ConcurrentHashMap<>();
        this.eventDispatcher = new AnimationEventDispatcher();
    }

    void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
        EntityLivingBase entity = event.getEntity();
        if (entity == null) {
            return;
        }

        String mappingName = getEntityCustomName(entity);
        EntityModelMapping mapping = null;
        if (mappingName != null && !mappingName.isEmpty()) {
            mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        }
        if (mapping == null && entity instanceof EntityPlayer) {
            String playerName = entity.getName();
            mapping = SkyCoreConfig.getInstance().getMapping(playerName);
            if (mapping != null) {
                mappingName = playerName;
            } else {
                mapping = SkyCoreConfig.getInstance().getMapping("player");
                if (mapping != null) {
                    mappingName = "player";
                }
            }
        }
        if (mapping == null) {
            return;
        }

        event.setCanceled(true);

        WrapperEntry entry = getOrCreateEntry(entity, mappingName, mapping);
        if (entry == null || entry.wrapper == null) {
            return;
        }

        Animation forced = getForcedAnimation(mappingName);
        List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
        if (forced != null) {
            entry.wrapper.setAnimation(forced);
            entry.wrapper.clearOverlayStates();
        } else if (entry.controller != null) {
            EntityAnimationController.Frame frame = entry.controller.update(entity);
            if (frame != null) {
                boolean override = false;
                if (frame.primary != null) {
                    entry.wrapper.setAnimation(frame.primary);
                    override = frame.primary.isOverridePreviousAnimation();
                    if (override) {
                        entry.wrapper.clearOverlayStates();
                    }
                }
                if (!override) {
                    entry.wrapper.setOverlayStates(frame.overlays);
                    overlayStates = frame.overlays != null ? frame.overlays : Collections.emptyList();
                }
            } else {
                entry.wrapper.clearOverlayStates();
            }
        } else {
            entry.wrapper.clearOverlayStates();
        }
        entry.overlayStates = overlayStates;

        renderEntity(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
    }

    void clearCache() {
        for (WrapperEntry entry : modelWrapperCache.values()) {
            entry.wrapper.dispose();
        }
        modelWrapperCache.clear();
        clearAllForcedAnimations();
    }

    void invalidateWrapper(String entityName) {
        for (java.util.Iterator<Map.Entry<Integer, WrapperEntry>> it = modelWrapperCache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WrapperEntry> entry = it.next();
            if (entityName.equals(entry.getValue().mappingName)) {
                entry.getValue().wrapper.dispose();
                it.remove();
            }
        }
    }

    void cleanupEntityWrappers() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        for (java.util.Iterator<Map.Entry<Integer, WrapperEntry>> it = modelWrapperCache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WrapperEntry> entry = it.next();
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (entity == null || entity.isDead) {
                entry.getValue().wrapper.dispose();
                it.remove();
            }
        }
    }

    boolean setForcedAnimation(String mappingName, Animation animation) {
        if (mappingName == null || mappingName.isEmpty() || animation == null) {
            return false;
        }
        forcedAnimations.put(mappingName, animation);
        for (WrapperEntry entry : modelWrapperCache.values()) {
            if (mappingName.equals(entry.mappingName)) {
                entry.wrapper.setAnimation(animation);
                entry.wrapper.clearOverlayStates();
            }
        }
        return true;
    }

    void clearForcedAnimation(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return;
        }
        forcedAnimations.remove(mappingName);
    }

    void clearAllForcedAnimations() {
        forcedAnimations.clear();
    }

    private Animation getForcedAnimation(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return null;
        }
        return forcedAnimations.get(mappingName);
    }

    private void renderEntity(EntityLivingBase entity, WrapperEntry entry,
                              double x, double y, double z, float partialTicks) {
        BedrockModelWrapper wrapper = entry.wrapper;
        float entityYaw = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
        eventDispatcher.dispatchAnimationEvents(entity, entry, wrapper, entityYaw, partialTicks);

        if (shouldRenderNameTag(entity)) {
            renderNameTag(entity, x, y, z);
        }
    }

    private boolean shouldRenderNameTag(EntityLivingBase entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.player) {
            return false;
        }
        return entity.getAlwaysRenderNameTagForRender();
    }

    private void renderNameTag(EntityLivingBase entity, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager().renderEngine != null) {
            double yOffset = y + entity.height + 0.5;
            mc.getRenderManager().renderEntity(entity, x, yOffset, z, 0, 0, false);
        }
    }

    private WrapperEntry getOrCreateEntry(EntityLivingBase entity, String entityName, EntityModelMapping mapping) {
        int entityId = entity.getEntityId();
        long tick = entity.world != null ? entity.world.getTotalWorldTime() : 0L;
        WrapperEntry entry = modelWrapperCache.get(entityId);
        if (entry != null) {
            if (!entity.getUniqueID().equals(entry.entityUuid) || !entityName.equals(entry.mappingName)) {
                entry.wrapper.dispose();
                modelWrapperCache.remove(entityId);
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

        BedrockModelWrapper wrapper = new BedrockModelWrapper(
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
        modelWrapperCache.put(entityId, created);

        SkyCoreMod.LOGGER.info("[SkyCore] 为实体 '{}' 创建模型包装器", entityName);
        return created;
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

    private String getEntityCustomName(Entity entity) {
        if (!entity.hasCustomName()) {
            return null;
        }
        return entity.getCustomNameTag();
    }

    private float interpolateRotation(float prev, float current, float partialTicks) {
        float diff = current - prev;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return prev + partialTicks * diff;
    }

    static final class EventCursor {
        float lastTime;
        int lastLoop;
        boolean valid;
    }

    static final class WrapperEntry {
        final BedrockModelWrapper wrapper;
        final EntityAnimationController controller;
        final UUID entityUuid;
        final String mappingName;
        long lastSeenTick;
        List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
        final Map<Animation, EventCursor> overlayCursors = new HashMap<>();
        Animation lastPrimaryAnimation;
        float lastPrimaryTime;
        int lastPrimaryLoop;
        boolean primaryValid;

        WrapperEntry(BedrockModelWrapper wrapper, EntityAnimationController controller, UUID entityUuid, String mappingName, long lastSeenTick) {
            this.wrapper = wrapper;
            this.controller = controller;
            this.entityUuid = entityUuid;
            this.mappingName = mappingName;
            this.lastSeenTick = lastSeenTick;
        }
    }
}
