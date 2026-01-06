package org.mybad.minecraft.event;

import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.debug.DebugStackManager;
import org.mybad.minecraft.render.BloomRenderer;
import org.mybad.minecraft.render.GLDeletionQueue;
import org.mybad.minecraft.render.entity.EntityRenderDispatcher;
import org.mybad.minecraft.resource.ResourceCacheManager;

/**
 * 渲染事件处理器
 * 拦截实体渲染事件，根据实体名字匹配配置
 * 使用 SkyCore Bedrock 模型替换原版渲染
 */
@SideOnly(Side.CLIENT)
public class EntityRenderEventHandler {

    private final EntityRenderDispatcher entityDispatcher;
    private final DebugStackManager debugStackManager;

    public EntityRenderEventHandler(ResourceCacheManager cacheManager) {
        this.entityDispatcher = new EntityRenderDispatcher(cacheManager);
        this.debugStackManager = new DebugStackManager(cacheManager);
    }

    /**
     * 渲染前事件 - 检查是否需要替换渲染
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
        entityDispatcher.onRenderLivingPre(event);
    }

    /**
     * 渲染调试模型
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        GLDeletionQueue.flush();
        entityDispatcher.cleanupEntityWrappers();
        debugStackManager.onRenderWorldLast(event);
        BloomRenderer.get().endFrame();
    }

    /**
     * 清空模型包装器缓存
     * 用于配置 reload 时
     */
    public void clearCache() {
        entityDispatcher.clearCache();
        debugStackManager.clearDebugStacks();
        SkyCoreMod.LOGGER.info("[SkyCore] 模型包装器缓存已清空");
    }

    /**
     * 从缓存中移除指定实体的包装器
     */
    public void invalidateWrapper(String entityName) {
        entityDispatcher.invalidateWrapper(entityName);
    }

    public void clearDebugStacks() {
        debugStackManager.clearDebugStacks();
    }

    public boolean setForcedAnimation(String mappingName, Animation animation) {
        return entityDispatcher.setForcedAnimation(mappingName, animation);
    }

    public void clearForcedAnimation(String mappingName) {
        entityDispatcher.clearForcedAnimation(mappingName);
    }

    public void clearAllForcedAnimations() {
        entityDispatcher.clearAllForcedAnimations();
    }

    public boolean addDebugStack(String mappingName, double x, double y, double z, float yaw, int count, double spacing) {
        return debugStackManager.addDebugStack(mappingName, x, y, z, yaw, count, spacing);
    }
}
