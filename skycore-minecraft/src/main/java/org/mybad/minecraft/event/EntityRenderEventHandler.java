package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.BedrockModelWrapper;
import org.mybad.minecraft.render.GLDeletionQueue;
import org.mybad.minecraft.render.entity.EntityAttachmentManager;
import org.mybad.minecraft.render.entity.EntityRenderDispatcher;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.render.entity.events.AnimationEventMathUtil;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.mybad.minecraft.render.trail.WeaponTrailRenderer;
import org.mybad.minecraft.resource.ResourceCacheManager;

/**
 * 渲染事件处理器
 * 拦截实体渲染事件，根据实体名字匹配配置
 * 使用 SkyCore Bedrock 模型替换原版渲染
 */
@SideOnly(Side.CLIENT)
public class EntityRenderEventHandler {

    private final EntityRenderDispatcher entityDispatcher;
    private final WeaponTrailRenderer weaponTrailRenderer;

    public EntityRenderEventHandler(ResourceCacheManager cacheManager) {
        this.weaponTrailRenderer = new WeaponTrailRenderer();
        this.entityDispatcher = new EntityRenderDispatcher(cacheManager, weaponTrailRenderer);
    }

    /**
     * 渲染前事件 - 检查是否需要替换渲染
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
        entityDispatcher.onRenderLivingPre(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Pre event) {
        EntityLivingBase entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (entityDispatcher.isSkyCoreEntity(entity)) {
            entity.setAlwaysRenderNameTag(false);
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<?> event) {
        EntityLivingBase entity = event.getEntity();
        if (entity == null) {
            return;
        }
        EntityAttachmentManager manager = entityDispatcher.getAttachmentManager();
        if (manager == null || !manager.hasAttachments(entity.getUniqueID())) {
            return;
        }
        if (entityDispatcher.isSkyCoreEntity(entity)) {
            return;
        }
        float partialTicks = event.getPartialRenderTick();
        float yaw = AnimationEventMathUtil.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
        AnimationEventDispatcher dispatcher = entityDispatcher.getEventDispatcher();
        manager.renderAttachments(entity,
            event.getX(),
            event.getY(),
            event.getZ(),
            yaw,
            partialTicks,
            dispatcher,
            weaponTrailRenderer);
    }

    /**
     * 渲染调试模型
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.isGamePaused()) {
            return;
        }
        GLDeletionQueue.flush();
        entityDispatcher.cleanupEntityWrappers();
        weaponTrailRenderer.render(event.getPartialTicks());
        entityDispatcher.getHeadBarManager().renderQueued(event.getPartialTicks());
        org.mybad.minecraft.debug.DebugRenderOverlay.render(event, entityDispatcher);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.isGamePaused()) {
            return;
        }
        entityDispatcher.onClientTick();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.isGamePaused()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            entityDispatcher.beginRenderFrame();
            SkullModelManager.beginRenderFrame();
            weaponTrailRenderer.beginFrame();
            entityDispatcher.getHeadBarManager().beginFrame();
        } else {
            entityDispatcher.finishRenderFrame();
            SkullModelManager.finishRenderFrame();
        }
    }

    /**
     * 清空模型包装器缓存
     * 用于配置 reload 时
     */
    public void clearCache() {
        entityDispatcher.clearCache();
        BedrockModelWrapper.clearSharedResources();
        SkyCoreMod.LOGGER.info("[SkyCore] 模型包装器缓存已清空");
        weaponTrailRenderer.beginFrame();
    }

    /**
     * 从缓存中移除指定实体的包装器
     */
    public void invalidateWrapper(String entityName) {
        entityDispatcher.invalidateWrapper(entityName);
    }
    
    public boolean setForcedAnimation(java.util.UUID entityUuid, Animation animation) {
        return entityDispatcher.setForcedAnimation(entityUuid, animation);
    }

    public boolean isSkyCoreEntity(net.minecraft.entity.EntityLivingBase entity) {
        return entityDispatcher.isSkyCoreEntity(entity);
    }

    public boolean isSkyCoreEntity(net.minecraft.entity.Entity entity) {
        return entityDispatcher.isSkyCoreEntity(entity);
    }

    public void clearForcedAnimation(java.util.UUID entityUuid) {
        entityDispatcher.clearForcedAnimation(entityUuid);
    }

    public void clearAllForcedAnimations() {
        entityDispatcher.clearAllForcedAnimations();
    }

    public String resolveMappingByEntity(java.util.UUID uuid) {
        return entityDispatcher.findMappingByUuid(uuid);
    }

    public EntityAttachmentManager getAttachmentManager() {
        return entityDispatcher.getAttachmentManager();
    }

    public void applyAttributeOverrides(java.util.List<org.mybad.skycoreproto.SkyCoreProto.EntityAttributeOverride> overrides) {
        entityDispatcher.applyAttributeOverrides(overrides);
    }

}
