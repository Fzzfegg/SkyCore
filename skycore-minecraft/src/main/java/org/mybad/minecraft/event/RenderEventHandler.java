package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.particle.ParticleEngine;
import org.mybad.minecraft.render.BedrockModelWrapper;
import org.mybad.minecraft.resource.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渲染事件处理器
 * 拦截实体渲染事件，根据实体名字匹配配置
 * 使用 SkyCore Bedrock 模型替换原版渲染
 */
@SideOnly(Side.CLIENT)
public class RenderEventHandler {

    /** 资源加载器 */
    private final ResourceLoader resourceLoader;

    /** 模型包装器缓存: 实体ID -> 包装器条目 */
    private final Map<Integer, WrapperEntry> modelWrapperCache;
    private final List<DebugStack> debugStacks;
    private final Map<String, Animation> forcedAnimations;
    private final ParticleEngine particleEngine;

    private static final float EVENT_EPS = 1.0e-4f;

    public RenderEventHandler(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.modelWrapperCache = new ConcurrentHashMap<>();
        this.debugStacks = new ArrayList<>();
        this.forcedAnimations = new ConcurrentHashMap<>();
        this.particleEngine = new ParticleEngine(resourceLoader);
    }



    /** 调试计时 */
    private long lastDebugTime = 0;

    /**
     * 渲染前事件 - 检查是否需要替换渲染
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();

        // 获取实体映射名
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
            return;  // 没有配置，使用原版渲染
        }
        
        // 取消原版渲染
        event.setCanceled(true);

        // 获取或创建模型包装器
        WrapperEntry entry = getOrCreateEntry(entity, mappingName, mapping);
        if (entry == null || entry.wrapper == null) {
            return;  // 加载失败，跳过渲染
        }
        Animation forced = getForcedAnimation(mappingName);
        List<EntityAnimationController.OverlayState> overlayStates = java.util.Collections.emptyList();
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
                    overlayStates = frame.overlays != null ? frame.overlays : java.util.Collections.emptyList();
                }
            } else {
                entry.wrapper.clearOverlayStates();
            }
        } else {
            entry.wrapper.clearOverlayStates();
        }
        entry.overlayStates = overlayStates;
        
        // 使用 SkyCore 渲染
        renderEntity(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
    }

    /**
     * 渲染调试模型
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        org.mybad.minecraft.render.GLDeletionQueue.flush();
        cleanupEntityWrappers();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null) {
            return;
        }
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        float partialTicks = event.getPartialTicks();

        particleEngine.updateAndRender(camX, camY, camZ);
        if (debugStacks.isEmpty()) {
            return;
        }

        synchronized (debugStacks) {
            for (DebugStack stack : debugStacks) {
                for (int i = 0; i < stack.count; i++) {
                    double y = stack.y + i * stack.spacing;
                    stack.wrapper.render(null,
                        stack.x - camX,
                        y - camY,
                        stack.z - camZ,
                        stack.yaw,
                        partialTicks
                    );
                }
            }
        }
    }

    /**
     * 获取实体自定义名字
     */
    private String getEntityCustomName(Entity entity) {
        if (!entity.hasCustomName()) {
            return null;
        }
        return entity.getCustomNameTag();
    }

    /**
     * 获取或创建模型包装器
     */
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

        // 加载模型
        Model model = resourceLoader.loadModel(mapping.getModel());
        if (model == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型: {} for entity: {}", mapping.getModel(), entityName);
            return null;
        }

        // 加载动画（可选）
        Animation animation = null;
        if (mapping.getAnimation() != null && !mapping.getAnimation().isEmpty()) {
            animation = resourceLoader.loadAnimation(mapping.getAnimation());
        }

        // 获取纹理
        ResourceLocation texture = resourceLoader.getTextureLocation(mapping.getTexture());
        ResourceLocation emissiveTexture = null;
        if (mapping.getEmissive() != null && !mapping.getEmissive().isEmpty()) {
            emissiveTexture = resourceLoader.getTextureLocation(mapping.getEmissive());
        }

        // 创建包装器，传入配置中的背面剔除设置
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
        modelWrapperCache.put(entityId, new WrapperEntry(wrapper, controller, entity.getUniqueID(), entityName, tick));

        SkyCoreMod.LOGGER.info("[SkyCore] 为实体 '{}' 创建模型包装器", entityName);
        return modelWrapperCache.get(entityId);
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

    /**
     * 使用 SkyCore 渲染实体
     */
    private void renderEntity(EntityLivingBase entity, WrapperEntry entry,
                              double x, double y, double z, float partialTicks) {
        BedrockModelWrapper wrapper = entry.wrapper;
        // 计算实体朝向
        float entityYaw = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        // 渲染模型
        wrapper.render(entity, x, y, z, entityYaw, partialTicks);

        // 触发动画事件（声音/粒子）
        dispatchAnimationEvents(entity, entry, wrapper, entityYaw, partialTicks);

        // 渲染名字标签（如果需要）
        if (shouldRenderNameTag(entity)) {
            renderNameTag(entity, x, y, z);
        }
    }

    /**
     * 插值旋转角度
     */
    private float interpolateRotation(float prev, float current, float partialTicks) {
        float diff = current - prev;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return prev + partialTicks * diff;
    }

    /**
     * 是否应该渲染名字标签
     */
    private boolean shouldRenderNameTag(EntityLivingBase entity) {
        Minecraft mc = Minecraft.getMinecraft();
        // 不渲染自己的名字
        if (entity == mc.player) {
            return false;
        }
        // 检查名字是否应该可见
        return entity.getAlwaysRenderNameTagForRender();
    }

    /**
     * 渲染名字标签
     */
    private void renderNameTag(EntityLivingBase entity, double x, double y, double z) {
        // 使用 Minecraft 默认的名字渲染
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager().renderEngine != null) {
            String name = entity.getDisplayName().getFormattedText();
            // 高度偏移
            double yOffset = y + entity.height + 0.5;
            mc.getRenderManager().renderEntity(entity, x, yOffset, z, 0, 0, false);
        }
    }

    private void dispatchAnimationEvents(EntityLivingBase entity, WrapperEntry entry, BedrockModelWrapper wrapper,
                                         float entityYaw, float partialTicks) {
        AnimationPlayer primaryPlayer = wrapper.getActiveAnimationPlayer();
        if (primaryPlayer != null && primaryPlayer.getAnimation() != null) {
            Animation animation = primaryPlayer.getAnimation();
            float currentTime = primaryPlayer.getState().getCurrentTime();
            int loopCount = primaryPlayer.getState().getLoopCount();
            if (!entry.primaryValid || entry.lastPrimaryAnimation != animation) {
                entry.lastPrimaryAnimation = animation;
                entry.lastPrimaryTime = currentTime;
                entry.lastPrimaryLoop = loopCount;
                entry.primaryValid = true;
            } else {
                boolean looped = loopCount != entry.lastPrimaryLoop ||
                    (animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < entry.lastPrimaryTime);
                dispatchEventsForAnimation(entity, wrapper, animation, entry.lastPrimaryTime, currentTime, looped, entityYaw, partialTicks);
                entry.lastPrimaryTime = currentTime;
                entry.lastPrimaryLoop = loopCount;
            }
        } else {
            entry.primaryValid = false;
        }

        if (entry.overlayStates == null || entry.overlayStates.isEmpty()) {
            entry.overlayCursors.clear();
            return;
        }

        java.util.Set<Animation> active = new java.util.HashSet<>();
        for (EntityAnimationController.OverlayState state : entry.overlayStates) {
            if (state == null || state.animation == null) {
                continue;
            }
            Animation animation = state.animation;
            active.add(animation);
            EventCursor cursor = entry.overlayCursors.get(animation);
            if (cursor == null) {
                cursor = new EventCursor();
                entry.overlayCursors.put(animation, cursor);
            }
            float currentTime = state.time;
            if (!cursor.valid) {
                cursor.lastTime = currentTime;
                cursor.lastLoop = 0;
                cursor.valid = true;
                continue;
            }
            boolean looped = animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < cursor.lastTime;
            dispatchEventsForAnimation(entity, wrapper, animation, cursor.lastTime, currentTime, looped, entityYaw, partialTicks);
            cursor.lastTime = currentTime;
        }

        entry.overlayCursors.keySet().removeIf(anim -> !active.contains(anim));
    }

    private void dispatchEventsForAnimation(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation animation,
                                            float prevTime, float currentTime, boolean looped,
                                            float entityYaw, float partialTicks) {
        if (animation == null) {
            return;
        }
        if (animation.getParticleEvents().isEmpty() && animation.getSoundEvents().isEmpty()) {
            return;
        }

        if (!looped && currentTime + EVENT_EPS < prevTime) {
            return;
        }

        dispatchEventList(entity, wrapper, animation, animation.getParticleEvents(), prevTime, currentTime, looped, entityYaw, partialTicks);
        dispatchEventList(entity, wrapper, animation, animation.getSoundEvents(), prevTime, currentTime, looped, entityYaw, partialTicks);
    }

    private void dispatchEventList(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation animation,
                                   List<Animation.Event> events,
                                   float prevTime, float currentTime, boolean looped,
                                   float entityYaw, float partialTicks) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if (!looped) {
            for (Animation.Event event : events) {
                if (event == null) {
                    continue;
                }
                float t = event.getTimestamp();
                if (t > prevTime + EVENT_EPS && t <= currentTime + EVENT_EPS) {
                    fireEvent(entity, wrapper, event, entityYaw, partialTicks);
                }
            }
            return;
        }

        float length = animation.getLength();
        for (Animation.Event event : events) {
            if (event == null) {
                continue;
            }
            float t = event.getTimestamp();
            if (t > prevTime + EVENT_EPS && t <= length + EVENT_EPS) {
                fireEvent(entity, wrapper, event, entityYaw, partialTicks);
            } else if (t >= -EVENT_EPS && t <= currentTime + EVENT_EPS) {
                fireEvent(entity, wrapper, event, entityYaw, partialTicks);
            }
        }
    }

    private void fireEvent(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation.Event event,
                           float entityYaw, float partialTicks) {
        double[] pos = resolveEventPosition(entity, wrapper, event.getLocator(), entityYaw, partialTicks);
        if (event.getType() == Animation.Event.Type.PARTICLE) {
            particleEngine.playEffect(event.getEffect(), pos[0], pos[1], pos[2]);
        } else if (event.getType() == Animation.Event.Type.SOUND) {
            playSoundEffect(event.getEffect(), pos[0], pos[1], pos[2]);
        }
    }

    private double[] resolveEventPosition(EntityLivingBase entity, BedrockModelWrapper wrapper, String locatorName,
                                          float entityYaw, float partialTicks) {
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        if (locatorName == null || locatorName.isEmpty()) {
            return new double[]{baseX, baseY, baseZ};
        }
        float[] local = wrapper.getLocatorPosition(locatorName);
        if (local == null) {
            return new double[]{baseX, baseY, baseZ};
        }
        float scale = wrapper.getModelScale();
        float lx = local[0] * scale;
        float ly = local[1] * scale;
        float lz = local[2] * scale;

        float yawRad = (float) Math.toRadians(180.0F - entityYaw);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float rx = lx * cos - lz * sin;
        float rz = lx * sin + lz * cos;
        return new double[]{baseX + rx, baseY + ly, baseZ + rz};
    }

    private void playSoundEffect(String effect, double x, double y, double z) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        SoundParams params = parseSoundParams(effect);
        if (params == null || params.soundEvent == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        mc.world.playSound(x, y, z, params.soundEvent, SoundCategory.NEUTRAL, params.volume, params.pitch, false);
    }

    private SoundParams parseSoundParams(String effect) {
        String soundId = null;
        float pitch = 1.0f;
        float volume = 1.0f;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (soundId == null) {
                    soundId = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("sound".equals(key)) {
                soundId = value;
            } else if ("pitch".equals(key)) {
                try {
                    pitch = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {}
            } else if ("volume".equals(key)) {
                try {
                    volume = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (soundId == null || soundId.isEmpty()) {
            soundId = effect;
        }
        soundId = soundId.trim();
        if (soundId.endsWith(".ogg")) {
            soundId = soundId.substring(0, soundId.length() - 4);
        }
        ResourceLocation soundLocation = parseSoundLocation(soundId);
        SoundEvent soundEvent = SoundEvent.REGISTRY.getObject(soundLocation);
        if (soundEvent == null) {
            soundEvent = new SoundEvent(soundLocation);
        }
        if (volume > 1.0f) {
            volume = volume / 100.0f;
        }
        SoundParams params = new SoundParams();
        params.soundEvent = soundEvent;
        params.volume = Math.max(0f, volume);
        params.pitch = Math.max(0f, pitch);
        return params;
    }

    private ResourceLocation parseSoundLocation(String soundId) {
        if (soundId.contains(":")) {
            return new ResourceLocation(soundId);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, soundId);
    }

    /**
     * 清空模型包装器缓存
     * 用于配置 reload 时
     */
    public void clearCache() {
        for (WrapperEntry entry : modelWrapperCache.values()) {
            entry.wrapper.dispose();
        }
        modelWrapperCache.clear();
        clearDebugStacks();
        clearAllForcedAnimations();
        particleEngine.clear();
        SkyCoreMod.LOGGER.info("[SkyCore] 模型包装器缓存已清空");
    }

    /**
     * 从缓存中移除指定实体的包装器
     */
    public void invalidateWrapper(String entityName) {
        for (java.util.Iterator<Map.Entry<Integer, WrapperEntry>> it = modelWrapperCache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WrapperEntry> entry = it.next();
            if (entityName.equals(entry.getValue().mappingName)) {
                entry.getValue().wrapper.dispose();
                it.remove();
            }
        }
    }

    public void clearDebugStacks() {
        synchronized (debugStacks) {
            for (DebugStack stack : debugStacks) {
                stack.wrapper.dispose();
            }
            debugStacks.clear();
        }
    }

    public boolean setForcedAnimation(String mappingName, Animation animation) {
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

    public void clearForcedAnimation(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return;
        }
        forcedAnimations.remove(mappingName);
    }

    public void clearAllForcedAnimations() {
        forcedAnimations.clear();
    }

    private Animation getForcedAnimation(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return null;
        }
        return forcedAnimations.get(mappingName);
    }

    public boolean addDebugStack(String mappingName, double x, double y, double z, float yaw, int count, double spacing) {
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null) {
            return false;
        }

        Model model = resourceLoader.loadModel(mapping.getModel());
        if (model == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型: {} for debug stack", mapping.getModel());
            return false;
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
        wrapper.setEmissiveStrength(mapping.getEmissiveStrength());

        synchronized (debugStacks) {
            debugStacks.add(new DebugStack(wrapper, x, y, z, yaw, count, spacing));
        }
        return true;
    }

    private void cleanupEntityWrappers() {
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

    private static final class DebugStack {
        private final BedrockModelWrapper wrapper;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final int count;
        private final double spacing;

        private DebugStack(BedrockModelWrapper wrapper, double x, double y, double z, float yaw, int count, double spacing) {
            this.wrapper = wrapper;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.count = count;
            this.spacing = spacing;
        }
    }

    private static final class EventCursor {
        private float lastTime;
        private int lastLoop;
        private boolean valid;
    }

    private static final class SoundParams {
        private SoundEvent soundEvent;
        private float volume;
        private float pitch;
    }

    private static final class WrapperEntry {
        private final BedrockModelWrapper wrapper;
        private final EntityAnimationController controller;
        private final java.util.UUID entityUuid;
        private final String mappingName;
        private long lastSeenTick;
        private List<EntityAnimationController.OverlayState> overlayStates = java.util.Collections.emptyList();
        private final Map<Animation, EventCursor> overlayCursors = new java.util.HashMap<>();
        private Animation lastPrimaryAnimation;
        private float lastPrimaryTime;
        private int lastPrimaryLoop;
        private boolean primaryValid;

        private WrapperEntry(BedrockModelWrapper wrapper, EntityAnimationController controller, java.util.UUID entityUuid, String mappingName, long lastSeenTick) {
            this.wrapper = wrapper;
            this.controller = controller;
            this.entityUuid = entityUuid;
            this.mappingName = mappingName;
            this.lastSeenTick = lastSeenTick;
        }
    }
}
