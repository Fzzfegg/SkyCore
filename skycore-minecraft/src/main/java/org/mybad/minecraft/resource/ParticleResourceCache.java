package org.mybad.minecraft.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.ParticleParser;
import net.minecraft.util.ResourceLocation;
import org.mybad.bedrockparticle.particle.BedrockResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ParticleResourceCache {
    private final ResourceResolver resolver;
    private final Map<String, ParticleData> particleCache = new ConcurrentHashMap<>();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Particle");

    ParticleResourceCache(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    ParticleData loadParticle(String path) {
        String key = resolver.normalizePath(path);
        ParticleData cached = particleCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = resolver.readResourceAsString(key);
            if (jsonContent == null) {
                reporter.missing(key);
                return null;
            }
            JsonElement root = new JsonParser().parse(jsonContent);
            ParticleData data = ParticleParser.parseParticle(root);
            patchParticleTextureNamespace(key, root, data);
            particleCache.put(key, data);
            return data;
        } catch (Exception e) {
            reporter.parseFailed(key, e);
            return null;
        }
    }

    void invalidateParticle(String path) {
        particleCache.remove(resolver.normalizePath(path));
    }

    int getCachedParticleCount() {
        return particleCache.size();
    }

    void clear() {
        particleCache.clear();
    }

    private void patchParticleTextureNamespace(String particlePath, JsonElement root, ParticleData data) {
        if (root == null || !root.isJsonObject() || data == null) {
            return;
        }
        String textureText = extractTexturePath(root.getAsJsonObject());
        if (textureText == null || textureText.contains(":")) {
            return;
        }
        if (!textureText.endsWith(".png")) {
            textureText += ".png";
        }
        ResourceLocation fileLoc = resolver.resolveResourceLocation(particlePath);
        String namespace = fileLoc.getNamespace();
        data.setTexture(new BedrockResourceLocation(namespace, textureText));
    }

    private String extractTexturePath(JsonObject root) {
        if (!root.has("particle_effect")) {
            return null;
        }
        JsonObject effect = root.getAsJsonObject("particle_effect");
        if (!effect.has("description")) {
            return null;
        }
        JsonObject desc = effect.getAsJsonObject("description");
        if (!desc.has("basic_render_parameters")) {
            return null;
        }
        JsonObject params = desc.getAsJsonObject("basic_render_parameters");
        if (!params.has("texture")) {
            return null;
        }
        try {
            return params.get("texture").getAsString();
        } catch (Exception ex) {
            return null;
        }
    }
}
