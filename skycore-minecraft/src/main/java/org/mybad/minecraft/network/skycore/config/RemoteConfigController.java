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
    }

    public void handleConfigIndex(SkyCoreProto.ConfigIndex index) {
        currentHash = index.getFullHash().toByteArray();
        cacheManager.saveIndex(index);
        SkyCoreMod.LOGGER.info("[SkyCore] Received config index with {} files", index.getEntriesCount());
    }

    public void handleMappingFile(SkyCoreProto.MappingFile file) {
        mappingFiles.put(file.getFileName(), file);
        cacheManager.saveMappingFile(file);
        SkyCoreMod.LOGGER.info("[SkyCore] Updated mapping file {}", file.getFileName());
        applyMappings();
    }

    public void handleFileRemoved(SkyCoreProto.ConfigFileRemoved removed) {
        mappingFiles.remove(removed.getFileName());
        cacheManager.deleteMappingFile(removed.getFileName());
        applyMappings();
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

    public byte[] getCurrentHash() {
        return currentHash;
    }
}
