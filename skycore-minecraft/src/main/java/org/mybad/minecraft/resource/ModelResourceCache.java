package org.mybad.minecraft.resource;

import org.mybad.core.data.Model;
import org.mybad.core.parsing.ModelParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModelResourceCache {
    private final ResourceResolver resolver;
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final ModelParser modelParser = new ModelParser();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Model");

    ModelResourceCache(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    Model loadModel(String path) {
        String key = resolver.normalizePath(path);
        Model cached = modelCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = resolver.readResourceAsString(key);
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
        modelCache.remove(resolver.normalizePath(path));
    }

    int getCachedModelCount() {
        return modelCache.size();
    }

    void clear() {
        modelCache.clear();
    }
}
