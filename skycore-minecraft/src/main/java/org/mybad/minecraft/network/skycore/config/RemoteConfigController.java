package org.mybad.minecraft.network.skycore.config;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import com.google.protobuf.InvalidProtocolBufferException;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.render.entity.HeadBarConfigStore;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.mybad.skycoreproto.SkyCoreProto;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class RemoteConfigController {

    private static final RemoteConfigController INSTANCE = new RemoteConfigController();
    private static final String HEADBAR_CONFIG_FILE = ConfigCacheManager.HEADBAR_FILE;

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
    }

    public void handleMappingFile(SkyCoreProto.MappingFile file) {
        mappingFiles.put(file.getFileName(), file);
        cacheManager.saveMappingFile(file);
//        SkyCoreMod.LOGGER.info("[SkyCore] 配置文件 {} 已更新。", file.getFileName());
        applyMappings();
    }

    public void handleFileRemoved(SkyCoreProto.ConfigFileRemoved removed) {
        mappingFiles.remove(removed.getFileName());
        cacheManager.deleteMappingFile(removed.getFileName());
        if (HEADBAR_CONFIG_FILE.equals(removed.getFileName())) {
            HeadBarConfigStore.clear();
            cacheManager.deleteHeadBarConfig();
        }
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
        syncHeadBarConfig();
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler != null) {
            handler.clearCache();
        }
        SkullModelManager.clear();
    }

    private void syncHeadBarConfig() {
        SkyCoreProto.MappingFile headBarFile = mappingFiles.get(HEADBAR_CONFIG_FILE);
        if (headBarFile == null || headBarFile.getRawPayload().isEmpty()) {
            HeadBarConfigStore.clear();
            cacheManager.deleteHeadBarConfig();
            return;
        }
        try {
            SkyCoreProto.HeadBarConfig config = SkyCoreProto.HeadBarConfig.parseFrom(headBarFile.getRawPayload());
            HeadBarConfigStore.update(config);
            cacheManager.writeHeadBarConfig(headBarFile.getRawPayload().toByteArray());
//            SkyCoreMod.LOGGER.info("[SkyCore] HeadBar 配置同步完成，共 {} 条规则。", config.getDefinitionsCount());
        } catch (InvalidProtocolBufferException ex) {
            SkyCoreMod.LOGGER.warn("[SkyCore] HeadBar 配置解析失败: {}", ex.getMessage());
        }
    }

    public byte[] getCurrentHash() {
        return currentHash;
    }
}
