package org.mybad.minecraft.resource;

import org.mybad.core.data.Model;
import org.mybad.core.parsing.ModelParser;
import org.mybad.minecraft.SkyCoreMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModelResourceLoader {
    private final ResourceLoader owner;
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final ModelParser modelParser = new ModelParser();

    ModelResourceLoader(ResourceLoader owner) {
        this.owner = owner;
    }

    Model loadModel(String path) {
        Model cached = modelCache.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = owner.loadResourceAsString(path);
            if (jsonContent == null) {
                SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型文件: {}", path);
                return null;
            }
            Model model = modelParser.parse(jsonContent);
            modelCache.put(path, model);
            return model;
        } catch (Exception e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 解析模型文件失败: {} - {}", path, e.getMessage());
            return null;
        }
    }

    void invalidateModel(String path) {
        modelCache.remove(path);
    }

    int getCachedModelCount() {
        return modelCache.size();
    }

    void clear() {
        modelCache.clear();
    }
}
