package org.mybad.minecraft.render.skull;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.render.entity.events.AnimationEventContext;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.render.entity.events.AnimationEventState;
import org.mybad.minecraft.render.entity.events.AnimationEventTarget;
import org.mybad.minecraft.render.entity.events.OverlayEventCursorCache;
import org.mybad.minecraft.resource.ResourceCacheManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SkullModelInstance implements AnimationEventContext {
    private final String mappingName;
    private final EntityModelMapping mapping;
    private final BedrockModelHandle handle;
    private final Map<String, Animation> animations;
    private final AnimationEventState eventState = new AnimationEventState();
    private final OverlayEventCursorCache overlayCursors = new OverlayEventCursorCache();
    private final SkullAnimationEventTarget eventTarget = new SkullAnimationEventTarget();
    private static final List<EntityAnimationController.OverlayState> NO_OVERLAYS = Collections.emptyList();
    private String activeClip;
    private String forcedClip;
    private boolean forcedOnce;
    private String defaultClip;
    private long lastSeenTick;
    private double lastWorldX;
    private double lastWorldY;
    private double lastWorldZ;
    private float lastYaw;
    private long lastRenderFrameId = Long.MIN_VALUE;

    private SkullModelInstance(String mappingName,
                               EntityModelMapping mapping,
                               BedrockModelHandle handle,
                               Map<String, Animation> animations,
                               long lastSeenTick) {
        this.mappingName = mappingName;
        this.mapping = mapping;
        this.handle = handle;
        this.animations = animations != null ? animations : Collections.emptyMap();
        this.lastSeenTick = lastSeenTick;
    }

    static SkullModelInstance create(ResourceCacheManager cacheManager,
                                     EntityModelMapping mapping,
                                     String mappingName,
                                     long tick) {
        BedrockModelHandle handle = ModelHandleFactory.create(cacheManager, mapping);
        if (handle == null) {
            return null;
        }
        applyMappingAttributes(handle, mapping);
        Map<String, Animation> animationSet = buildAnimationSet(cacheManager, mapping.getAnimation());
        SkullModelInstance instance = new SkullModelInstance(mappingName, mapping, handle, animationSet, tick);
        instance.ensureDefaultClip();
        return instance;
    }

    String getMappingName() {
        return mappingName;
    }

    void dispose() {
        handle.dispose();
    }

    void markSeen(long tick) {
        this.lastSeenTick = tick;
    }

    long getLastSeenTick() {
        return lastSeenTick;
    }

    void applyProfile() {
        if (!applyForcedClipIfNeeded()) {
            applyDefaultClip();
        }
        handle.setModelScale(mapping.getModelScale());
    }

    boolean forceClip(String clipName, boolean once) {
        if (clipName == null || clipName.trim().isEmpty() || animations.isEmpty()) {
            return false;
        }
        String key = clipName.toLowerCase(Locale.ROOT);
        Animation animation = animations.get(key);
        if (animation == null) {
            return false;
        }
        forcedClip = key;
        forcedOnce = once;
        if (key.equals(activeClip)) {
            handle.restartAnimation();
            return true;
        }
        activeClip = key;
        handle.setAnimation(animation);
        handle.restartAnimation();
        return true;
    }

    private boolean applyForcedClipIfNeeded() {
        if (forcedClip == null || forcedClip.isEmpty()) {
            return false;
        }
        if (!applyClip(forcedClip)) {
            clearForcedClip();
            return false;
        }
        if (!forcedOnce) {
            return true;
        }
        AnimationPlayer player = handle.getActiveAnimationPlayer();
        if (player != null && player.isFinished()) {
            clearForcedClip();
            return false;
        }
        return true;
    }

    private void clearForcedClip() {
        forcedClip = null;
        forcedOnce = false;
    }

    private void applyDefaultClip() {
        if (defaultClip == null || defaultClip.isEmpty()) {
            return;
        }
        Animation animation = animations.get(defaultClip);
        if (animation == null) {
            return;
        }
        if (defaultClip.equals(activeClip)) {
            return;
        }
        activeClip = defaultClip;
        handle.setAnimation(animation);
        handle.restartAnimation();
    }

    void render(double renderX,
                double renderY,
                double renderZ,
                double worldX,
                double worldY,
                double worldZ,
                float yaw,
                float partialTicks) {
        handle.updateAnimations();
        eventTarget.update(worldX, worldY, worldZ, yaw);
        this.lastWorldX = worldX;
        this.lastWorldY = worldY;
        this.lastWorldZ = worldZ;
        this.lastYaw = yaw;
        handle.renderBlock(renderX, renderY, renderZ, yaw, partialTicks);
    }

    void dispatchEvents(AnimationEventDispatcher dispatcher, float partialTicks) {
        if (dispatcher == null) {
            return;
        }
        dispatcher.dispatchAnimationEvents(null, this, eventTarget, handle, partialTicks);
    }

    SkullModelManager.DebugInfo toDebugInfo() {
        return new SkullModelManager.DebugInfo(
            mappingName,
            lastWorldX,
            lastWorldY,
            lastWorldZ,
            handle.getModelScale(),
            lastYaw
        );
    }

    void markRenderFrame(long frameId) {
        this.lastRenderFrameId = frameId;
    }

    boolean renderedThisFrame(long frameId) {
        return this.lastRenderFrameId == frameId;
    }

    void updateAnimationsFrame() {
        handle.updateAnimations();
    }

    private boolean applyClip(String clipName) {
        if (clipName == null || clipName.trim().isEmpty() || animations.isEmpty()) {
            return false;
        }
        String key = clipName.toLowerCase(Locale.ROOT);
        Animation animation = animations.get(key);
        if (animation == null) {
            return false;
        }
        if (key.equals(activeClip)) {
            return true;
        }
        activeClip = key;
        handle.setAnimation(animation);
        handle.restartAnimation();
        return true;
    }

    private void ensureDefaultClip() {
        if (animations.isEmpty()) {
            return;
        }
        if (defaultClip == null) {
            Map.Entry<String, Animation> entry = animations.entrySet().iterator().next();
            defaultClip = entry.getKey();
        }
        applyDefaultClip();
    }

    private static Map<String, Animation> buildAnimationSet(ResourceCacheManager cacheManager, String animationPath) {
        if (cacheManager == null || animationPath == null || animationPath.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Animation> loaded = cacheManager.loadAnimationSet(animationPath);
        if (loaded == null || loaded.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Animation> normalized = new HashMap<>();
        for (Map.Entry<String, Animation> entry : loaded.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    private static void applyMappingAttributes(BedrockModelHandle handle, EntityModelMapping mapping) {
        if (handle == null || mapping == null) {
            return;
        }
        handle.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        handle.setEmissiveStrength(mapping.getEmissiveStrength());
        handle.setBloomStrength(mapping.getBloomStrength());
        handle.setBloomColor(mapping.getBloomColor());
        handle.setBloomPasses(mapping.getBloomPasses());
        handle.setBloomScaleStep(mapping.getBloomScaleStep());
        handle.setBloomDownscale(mapping.getBloomDownscale());
        handle.setBloomOffset(mapping.getBloomOffset());
        handle.setModelScale(mapping.getModelScale());
        handle.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());
        handle.setRenderHurtTint(mapping.isRenderHurtTint());
        handle.setHurtTint(mapping.getHurtTint());
        handle.setLightning(mapping.isLightning());
    }

    @Override
    public AnimationEventState getPrimaryEventState() {
        return eventState;
    }

    @Override
    public List<EntityAnimationController.OverlayState> getOverlayStates() {
        return NO_OVERLAYS;
    }

    @Override
    public OverlayEventCursorCache getOverlayCursorCache() {
        return overlayCursors;
    }

    private static final class SkullAnimationEventTarget implements AnimationEventTarget {
        private double baseX;
        private double baseY;
        private double baseZ;
        private float yaw;

        void update(double x, double y, double z, float yaw) {
            this.baseX = x;
            this.baseY = y;
            this.baseZ = z;
            this.yaw = yaw;
        }

        @Override
        public double getBaseX() {
            return baseX;
        }

        @Override
        public double getBaseY() {
            return baseY;
        }

        @Override
        public double getBaseZ() {
            return baseZ;
        }

        @Override
        public float getHeadYaw() {
            return yaw;
        }

        @Override
        public float getBodyYaw() {
            return yaw;
        }
    }
}
