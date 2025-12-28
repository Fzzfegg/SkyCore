package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
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

    /** 模型包装器缓存: 实体名字 -> ModelWrapper */
    private final Map<String, BedrockModelWrapper> modelWrapperCache;
    private final List<DebugStack> debugStacks;

    public RenderEventHandler(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.modelWrapperCache = new ConcurrentHashMap<>();
        this.debugStacks = new ArrayList<>();
    }



    /** 调试计时 */
    private long lastDebugTime = 0;

    /**
     * 渲染前事件 - 检查是否需要替换渲染
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();

        // 获取实体自定义名字
        String entityName = getEntityCustomName(entity);
        if (entityName == null || entityName.isEmpty()) {
            return;  // 没有自定义名字，使用原版渲染
        }
        
        // 检查配置中是否有该名字的映射
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(entityName);
        if (mapping == null) {
            return;  // 没有配置，使用原版渲染
        }
        
        // 取消原版渲染
        event.setCanceled(true);

        // 获取或创建模型包装器
        BedrockModelWrapper wrapper = getOrCreateWrapper(entityName, mapping);
        if (wrapper == null) {
            return;  // 加载失败，跳过渲染
        }
        
        // 使用 SkyCore 渲染
        renderEntity(entity, wrapper, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
    }

    /**
     * 渲染调试模型
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (debugStacks.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null) {
            return;
        }
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        float partialTicks = event.getPartialTicks();

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
    private BedrockModelWrapper getOrCreateWrapper(String entityName, EntityModelMapping mapping) {
        // 检查缓存
        if (modelWrapperCache.containsKey(entityName)) {
            return modelWrapperCache.get(entityName);
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

        // 创建包装器，传入配置中的背面剔除设置
        BedrockModelWrapper wrapper = new BedrockModelWrapper(
            model,
            animation,
            texture,
            mapping.isEnableCull(),
            mapping.getModel(),
            resourceLoader.getGeometryCache()
        );
        modelWrapperCache.put(entityName, wrapper);

        SkyCoreMod.LOGGER.info("[SkyCore] 为实体 '{}' 创建模型包装器", entityName);
        return wrapper;
    }

    /**
     * 使用 SkyCore 渲染实体
     */
    private void renderEntity(EntityLivingBase entity, BedrockModelWrapper wrapper,
                              double x, double y, double z, float partialTicks) {
        // 计算实体朝向
        float entityYaw = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        // 渲染模型
        wrapper.render(entity, x, y, z, entityYaw, partialTicks);

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

    /**
     * 清空模型包装器缓存
     * 用于配置 reload 时
     */
    public void clearCache() {
        for (BedrockModelWrapper wrapper : modelWrapperCache.values()) {
            wrapper.dispose();
        }
        modelWrapperCache.clear();
        clearDebugStacks();
        SkyCoreMod.LOGGER.info("[SkyCore] 模型包装器缓存已清空");
    }

    /**
     * 从缓存中移除指定实体的包装器
     */
    public void invalidateWrapper(String entityName) {
        BedrockModelWrapper wrapper = modelWrapperCache.remove(entityName);
        if (wrapper != null) {
            wrapper.dispose();
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
        BedrockModelWrapper wrapper = new BedrockModelWrapper(
            model,
            animation,
            texture,
            mapping.isEnableCull(),
            mapping.getModel(),
            resourceLoader.getGeometryCache()
        );

        synchronized (debugStacks) {
            debugStacks.add(new DebugStack(wrapper, x, y, z, yaw, count, spacing));
        }
        return true;
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
}
