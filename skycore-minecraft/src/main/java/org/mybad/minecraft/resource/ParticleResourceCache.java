package org.mybad.minecraft.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.ParticleParser;
import org.mybad.bedrockparticle.particle.io.ParticleBinarySerializer;
import net.minecraft.util.ResourceLocation;
import org.mybad.bedrockparticle.particle.BedrockResourceLocation;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;
import org.mybad.core.binary.BinaryResourceIO;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.binary.SkycoreBinaryArchive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ParticleResourceCache {
    private final ResourceResolver resolver;
    private final BinaryPayloadCipherRegistry cipherRegistry;
    private final Map<String, ParticleData> particleCache = new ConcurrentHashMap<>();
    private final ParticleBinarySerializer binarySerializer = new ParticleBinarySerializer();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Particle");

    ParticleResourceCache(ResourceResolver resolver, BinaryPayloadCipherRegistry cipherRegistry) {
        this.resolver = resolver;
        this.cipherRegistry = cipherRegistry != null ? cipherRegistry : BinaryPayloadCipherRegistry.withDefaults();
    }

    ParticleData loadParticle(String path) {
        String key = resolver.normalizePath(path);
        ParticleData cached = particleCache.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceResolver.ResourceLookup lookup = resolver.lookup(path, ResourceResolver.ResourceType.PARTICLE);
        if (lookup.hasBinary()) {
            ParticleData data = readBinaryParticle(lookup.getBinaryPath(), key);
            if (data != null) {
                particleCache.put(key, data);
                return data;
            }
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

    private ParticleData readBinaryParticle(Path path, String key) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            SkycoreBinaryArchive archive = BinaryResourceIO.read(bytes, cipherRegistry::resolve);
            if (archive.getHeader().getType() != BinaryResourceType.PARTICLE) {
                reporter.parseFailed(key, path, new IllegalStateException("Unexpected binary type " + archive.getHeader().getType()));
                return null;
            }
            BinaryDataReader reader = new BinaryDataReader(archive.getPayload());
            return binarySerializer.read(reader);
        } catch (Exception ex) {
            reporter.parseFailed(key, path, ex);
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
        if (textureText == null) {
            return;
        }
        ResourceLocation fileLoc = resolver.resolveResourceLocation(particlePath);
        String namespace = fileLoc.getNamespace();
        if (!textureText.contains(":")) {
            if (!textureText.endsWith(".png")) {
                textureText += ".png";
            }
            data.setTexture(new BedrockResourceLocation(namespace, textureText));
        }
        String emissiveText = extractEmissivePath(root.getAsJsonObject());
        if (emissiveText != null && !emissiveText.contains(":")) {
            if (!emissiveText.endsWith(".png")) {
                emissiveText += ".png";
            }
            data.setEmissiveTexture(new BedrockResourceLocation(namespace, emissiveText));
        }
        String blendText = extractBlendPath(root.getAsJsonObject());
        if (blendText != null && !blendText.contains(":")) {
            if (!blendText.endsWith(".png")) {
                blendText += ".png";
            }
            data.setBlendTexture(new BedrockResourceLocation(namespace, blendText));
        }
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

    private String extractEmissivePath(JsonObject root) {
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
        if (params.has("emissive_texture")) {
            try {
                return params.get("emissive_texture").getAsString();
            } catch (Exception ex) {
                return null;
            }
        }
        if (params.has("emissive")) {
            try {
                return params.get("emissive").getAsString();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private String extractBlendPath(JsonObject root) {
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
        if (params.has("blendTexture")) {
            try {
                return params.get("blendTexture").getAsString();
            } catch (Exception ex) {
                return null;
            }
        }
        if (params.has("blend_texture")) {
            try {
                return params.get("blend_texture").getAsString();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }
}
