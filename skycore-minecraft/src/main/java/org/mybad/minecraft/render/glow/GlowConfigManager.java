package org.mybad.minecraft.render.glow;

import org.mybad.minecraft.config.EntityModelMapping;

import java.util.*;

/**
 * Keeps track of all glow/bloom configurations derived from model mappings.
 */
public final class GlowConfigManager {
    public static final GlowConfigManager INSTANCE = new GlowConfigManager();

    private final Map<String, GlowConfig> configs = new LinkedHashMap<>();

    private GlowConfigManager() {}

    public synchronized void clear() {
        configs.clear();
    }

    public synchronized void updateFromMappings(Collection<EntityModelMapping> mappings) {
        configs.clear();
        if (mappings == null) {
            return;
        }
        for (EntityModelMapping mapping : mappings) {
            if (mapping == null || mapping.getName() == null) {
                continue;
            }
            GlowConfig config = GlowConfig.fromMapping(mapping);
            if (config == null) {
                continue;
            }
            configs.put(mapping.getName(), config);
        }
    }

    public synchronized void update(String key, GlowConfig config) {
        if (key == null || config == null) {
            return;
        }
        configs.put(key, config);
    }

    public synchronized void remove(String key) {
        if (key == null) {
            return;
        }
        configs.remove(key);
    }

    public synchronized boolean hasConfigs() {
        return !configs.isEmpty();
    }

    public synchronized GlowConfig findConfig(String entityName) {
        if (entityName == null || configs.isEmpty()) {
            return null;
        }
        GlowConfig best = null;
        int bestNameLength = -1;
        String lowered = entityName.toLowerCase(Locale.ROOT);
        for (GlowConfig config : configs.values()) {
            if (config == null || config.getName() == null) {
                continue;
            }
            String configName = config.getName().toLowerCase(Locale.ROOT);
            if (!lowered.contains(configName)) {
                continue;
            }
            if (configName.length() > bestNameLength) {
                best = config;
                bestNameLength = configName.length();
            }
        }
        return best;
    }

    public synchronized Collection<GlowConfig> getAllConfigs() {
        return Collections.unmodifiableCollection(new ArrayList<>(configs.values()));
    }
}
