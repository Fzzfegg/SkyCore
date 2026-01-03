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
        Model cached = modelCache.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = owner.readResourceAsString(path);
            if (jsonContent == null) {
                reporter.missing(path);
                return null;
            }
            Model model = modelParser.parse(jsonContent);
            modelCache.put(path, model);
            return model;
        } catch (Exception e) {
            reporter.parseFailed(path, e);
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
