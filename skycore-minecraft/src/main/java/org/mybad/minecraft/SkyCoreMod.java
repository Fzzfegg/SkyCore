package org.mybad.minecraft;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.event.RenderEventHandler;
import org.mybad.minecraft.resource.ResourceLoader;

/**
 * SkyCore Minecraft Mod 主类
 * 纯客户端 MOD - 根据实体名字替换为 Bedrock 模型渲染
 */
@Mod(
    modid = SkyCoreMod.MOD_ID,
    name = SkyCoreMod.MOD_NAME,
    version = SkyCoreMod.VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.12,1.13)"
)
public class SkyCoreMod {
    public static final String MOD_ID = "skycore";
    public static final String MOD_NAME = "SkyCore";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static SkyCoreMod instance;

    /** 资源加载器 */
    @SideOnly(Side.CLIENT)
    private ResourceLoader resourceLoader;

    /** 渲染事件处理器 */
    @SideOnly(Side.CLIENT)
    private RenderEventHandler renderEventHandler;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[SkyCore] PreInit - 初始化配置...");

        // 初始化配置
        SkyCoreConfig.init(event.getModConfigurationDirectory());

        // 初始化资源加载器
        resourceLoader = new ResourceLoader();

        LOGGER.info("[SkyCore] PreInit 完成");
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[SkyCore] Init - 注册事件处理器...");

        // 创建并注册渲染事件处理器
        renderEventHandler = new RenderEventHandler(resourceLoader);
        MinecraftForge.EVENT_BUS.register(renderEventHandler);

        // 注册 reload 命令处理器
        MinecraftForge.EVENT_BUS.register(new ReloadCommandHandler());

        LOGGER.info("[SkyCore] Init 完成");
    }

    /**
     * 重新加载配置和资源
     */
    @SideOnly(Side.CLIENT)
    public void reload() {
        LOGGER.info("[SkyCore] 重新加载...");

        // 重新加载配置
        SkyCoreConfig.getInstance().reload();

        // 清空资源缓存
        if (resourceLoader != null) {
            resourceLoader.clearCache();
        }

        // 清空模型包装器缓存
        if (renderEventHandler != null) {
            renderEventHandler.clearCache();
        }

        LOGGER.info("[SkyCore] 重新加载完成");
    }

    /**
     * 获取资源加载器
     */
    @SideOnly(Side.CLIENT)
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * 获取渲染事件处理器
     */
    @SideOnly(Side.CLIENT)
    public static RenderEventHandler getRenderEventHandler() {
        return instance != null ? instance.renderEventHandler : null;
    }

    /**
     * Reload 命令处理器
     * 监听聊天消息，支持 /skycore reload 命令
     */
    @SideOnly(Side.CLIENT)
    public static class ReloadCommandHandler {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onClientChat(net.minecraftforge.client.event.ClientChatEvent event) {
            String message = event.getMessage();
            if (message.equalsIgnoreCase("/skycore reload")) {
                if (instance != null) {
                    instance.reload();
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("\u00a7a[SkyCore] \u914d\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d")
                    );
                }
                return;
            }

            if (message.equalsIgnoreCase("/skycore geomstats")) {
                if (instance != null && instance.resourceLoader != null) {
                    org.mybad.minecraft.render.GeometryCache.Stats stats = instance.resourceLoader.getGeometryCache().getStats();
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 几何缓存统计: " + stats.toString())
                    );
                }
                return;
            }

            if (message.equalsIgnoreCase("/skycore debug_clear")) {
                if (instance != null && instance.renderEventHandler != null) {
                    instance.renderEventHandler.clearDebugStacks();
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(new net.minecraft.util.text.TextComponentString("[SkyCore] 调试堆叠已清空"));
                }
                return;
            }

            if (message.startsWith("/skycore debug_stack")) {
                if (instance == null || instance.renderEventHandler == null) {return;}
                String[] parts = message.trim().split("\\s+");
                if (parts.length < 3) {net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(new net.minecraft.util.text.TextComponentString("[SkyCore] 用法: /skycore debug_stack <名字> [数量] [间距]"));return;}
                String mappingName = parts[2];
                int count = 20;
                double spacing = 1.0;
                if (parts.length >= 4) {try {count = Integer.parseInt(parts[3]);} catch (NumberFormatException ignored) {}}
                if (parts.length >= 5) {try {spacing = Double.parseDouble(parts[4]);} catch (NumberFormatException ignored) {}}
                if (count < 1) {count = 1;} else if (count > 200) {count = 200;}
                if (spacing <= 0.0) {spacing = 1.0;}

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                if (mc.player == null) {
                    return;
                }
                double x = mc.player.posX;
                double y = mc.player.posY;
                double z = mc.player.posZ;
                float yaw = mc.player.rotationYaw;

                boolean ok = instance.renderEventHandler.addDebugStack(mappingName, x, y, z, yaw, count, spacing);
                if (ok) {
                    mc.player.sendMessage(new net.minecraft.util.text.TextComponentString("[SkyCore] 已创建调试堆叠: " + mappingName + "，数量=" + count + "，间距=" + spacing));
                } else {
                    mc.player.sendMessage(new net.minecraft.util.text.TextComponentString("[SkyCore] 创建调试堆叠失败：未找到映射或模型加载失败"));
                }
                return;
            }

            if (message.startsWith("/skycore play_anim_clear")) {
                if (instance == null || instance.renderEventHandler == null) {return;}
                String[] parts = message.trim().split("\\s+");
                if (parts.length < 3) {
                    instance.renderEventHandler.clearAllForcedAnimations();
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 已清空所有强制动画")
                    );
                    return;
                }
                String mappingName = parts[2];
                instance.renderEventHandler.clearForcedAnimation(mappingName);
                net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                    new net.minecraft.util.text.TextComponentString("[SkyCore] 已清除强制动画: " + mappingName)
                );
                return;
            }

            if (message.startsWith("/skycore play_anim")) {
                if (instance == null || instance.renderEventHandler == null || instance.resourceLoader == null) {return;}
                String[] parts = message.trim().split("\\s+");
                if (parts.length < 4) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 用法: /skycore play_anim <名字> <动画片段名>")
                    );
                    return;
                }
                String mappingName = parts[2];
                String clipName = parts[3];
                EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
                if (mapping == null) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 未找到映射: " + mappingName)
                    );
                    return;
                }
                String animPath = mapping.getAnimation();
                if (animPath == null || animPath.isEmpty()) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 该映射没有动画文件: " + mappingName)
                    );
                    return;
                }
                org.mybad.core.animation.Animation animation = instance.resourceLoader.loadAnimation(animPath, clipName);
                if (animation == null) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 动画片段不存在: " + clipName)
                    );
                    return;
                }
                boolean ok = instance.renderEventHandler.setForcedAnimation(mappingName, animation);
                if (ok) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 已强制播放动画: " + mappingName + " -> " + clipName)
                    );
                } else {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new net.minecraft.util.text.TextComponentString("[SkyCore] 强制播放失败")
                    );
                }
                return;
            }
        }
    }
}
