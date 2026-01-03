package org.mybad.minecraft.resource;

import org.mybad.core.data.Model;
import org.mybad.core.parsing.ModelParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModelResourceCache {
    private final ResourceLoader owner;
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final ModelParser modelParser = new ModelParser();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Model");

    ModelResourceCache(ResourceLoader owner) {
        this.owner = owner;
    }

    Model loadModel(String path) {
        String key = owner.normalizePath(path);
        Model cached = modelCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = owner.readResourceAsString(key);
            if (jsonContent == null) {
                reporter.missing(key);
                return null;
            }
            Model model = modelParser.parse(jsonContent);
            modelCache.put(key, model);
            return model;
        } catch (Exception e) {
            reporter.parseFailed(key, e);
            return null;
        }
    }

    void invalidateModel(String path) {
        modelCache.remove(owner.normalizePath(path));
    }

    int getCachedModelCount() {
        return modelCache.size();
    }

    void clear() {
        modelCache.clear();
    }
}
