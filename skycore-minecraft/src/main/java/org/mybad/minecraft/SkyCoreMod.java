package org.mybad.minecraft;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybad.minecraft.command.SkyCoreCommandHandler;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.audio.SoundExistenceCache;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.debug.BedrockParticleDebugSystem;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.resource.ResourceLoader;
import org.mybad.minecraft.resource.ResourcePackRegistrar;

import java.io.File;

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
    private EntityRenderEventHandler renderEventHandler;
    @SideOnly(Side.CLIENT)
    private BedrockParticleSystem particleSystem;
    @SideOnly(Side.CLIENT)
    private BedrockParticleDebugSystem particleDebugSystem;
    private File gameDir;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[SkyCore] PreInit - 初始化配置...");

        // 初始化配置
        this.gameDir = event.getModConfigurationDirectory().getParentFile();
        SkyCoreConfig.init(ResourcePackRegistrar.getPackRoot(gameDir));

        // 初始化资源加载器
        resourceLoader = new ResourceLoader();
        SoundExistenceCache.rescan(gameDir != null ? gameDir.toPath() : null);

        LOGGER.info("[SkyCore] PreInit 完成");
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[SkyCore] Init - 注册事件处理器...");

        // 注册虚拟资源包
        ResourcePackRegistrar.registerConfigPack(ResourcePackRegistrar.getPackRoot(gameDir));
        
        // 粒子系统（核心）
        particleSystem = new BedrockParticleSystem(resourceLoader);
        MinecraftForge.EVENT_BUS.register(particleSystem);
        // 粒子调试包装
        particleDebugSystem = new BedrockParticleDebugSystem(particleSystem);
        // 创建并注册渲染事件处理器
        renderEventHandler = new EntityRenderEventHandler(resourceLoader);
        MinecraftForge.EVENT_BUS.register(renderEventHandler);

        // 注册命令处理器
        MinecraftForge.EVENT_BUS.register(new SkyCoreCommandHandler());

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
        SoundExistenceCache.rescan(gameDir != null ? gameDir.toPath() : null);

        // 清空资源缓存
        resourceLoader.clearCache();
        renderEventHandler.clearCache();
        particleDebugSystem.clear();
        
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
    public static EntityRenderEventHandler getEntityRenderEventHandler() {
        return instance != null ? instance.renderEventHandler : null;
    }


    @SideOnly(Side.CLIENT)
    public static BedrockParticleSystem getParticleSystem() {
        return instance != null ? instance.particleSystem : null;
    }

    @SideOnly(Side.CLIENT)
    public static BedrockParticleDebugSystem getParticleDebugSystem() {
        return instance != null ? instance.particleDebugSystem : null;
    }


}
