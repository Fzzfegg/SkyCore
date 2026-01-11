package org.mybad.minecraft.network.skycore.config;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.skycoreproto.SkyCoreProto;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class RemoteConfigController {

    private static final RemoteConfigController INSTANCE = new RemoteConfigController();

    public static RemoteConfigController getInstance() {
        return INSTANCE;
    }

    private final ConfigCacheManager cacheManager;
    private final Map<String, SkyCoreProto.MappingFile> mappingFiles = Maps.newHashMap();

    private byte[] currentHash = new byte[0];

    private RemoteConfigController() {
        Path gameDir = Minecraft.getMinecraft().gameDir.toPath();
        this.cacheManager = new ConfigCacheManager(gameDir);
    }

    public void loadCacheOnStartup() {
        SkyCoreProto.ConfigIndex index = cacheManager.loadIndex();
        if (index != null) {
            currentHash = index.getFullHash().toByteArray();
        }
        for (SkyCoreProto.MappingFile file : cacheManager.loadAllFiles()) {
            mappingFiles.put(file.getFileName(), file);
        }
        applyMappings();
        SkyCoreProto.RenderSettings renderSettings = cacheManager.loadRenderSettings();
        if (renderSettings != null) {
            applyRenderSettings(renderSettings);
        }
    }

    public void handleConfigIndex(SkyCoreProto.ConfigIndex index) {
        currentHash = index.getFullHash().toByteArray();
        cacheManager.saveIndex(index);
        SkyCoreMod.LOGGER.info("[SkyCore] 已接收配置索引，共 {} 个文件。", index.getEntriesCount());
    }

    public void handleMappingFile(SkyCoreProto.MappingFile file) {
        mappingFiles.put(file.getFileName(), file);
        cacheManager.saveMappingFile(file);
        SkyCoreMod.LOGGER.info("[SkyCore] 配置文件 {} 已更新。", file.getFileName());
        applyMappings();
    }

    public void handleFileRemoved(SkyCoreProto.ConfigFileRemoved removed) {
        mappingFiles.remove(removed.getFileName());
        cacheManager.deleteMappingFile(removed.getFileName());
        applyMappings();
    }

    public void handleRenderSettings(SkyCoreProto.RenderSettings settings) {
        cacheManager.saveRenderSettings(settings);
        applyRenderSettings(settings);
    }

    private void applyMappings() {
        Map<String, EntityModelMapping> merged = Maps.newHashMap();
        for (SkyCoreProto.MappingFile file : mappingFiles.values()) {
            for (SkyCoreProto.EntityMapping mappingProto : file.getMappingsList()) {
                merged.put(mappingProto.getName(), ProtoMappingConverter.toEntityModelMapping(mappingProto));
            }
        }
        SkyCoreConfig.getInstance().applyRemoteMappings(merged);
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler != null) {
            handler.clearCache();
        }
    }

    private void applyRenderSettings(SkyCoreProto.RenderSettings settings) {
        SkyCoreConfig.getInstance().applyRenderSettings(
                settings.getBloomStrength(),
                settings.getBloomRadius(),
                settings.getBloomDownsample(),
                settings.getBloomThreshold(),
                settings.getBloomPasses()
        );
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler != null) {
            handler.clearCache();
        }
    }

    public byte[] getCurrentHash() {
        return currentHash;
    }
}
