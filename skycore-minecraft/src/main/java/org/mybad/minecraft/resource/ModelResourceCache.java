package org.mybad.minecraft.resource;

import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryResourceFlags;
import org.mybad.core.binary.BinaryResourceHeader;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;
import org.mybad.core.binary.BinaryResourceIO;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.binary.SkycoreBinaryArchive;
import org.mybad.core.binary.model.ModelBinarySerializer;
import org.mybad.core.data.Model;
import org.mybad.core.parsing.ModelParser;
import org.mybad.minecraft.network.skycore.SkycoreClientHandshake;

import java.security.GeneralSecurityException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModelResourceCache {
    private final ResourceResolver resolver;
    private final BinaryPayloadCipherRegistry cipherRegistry;
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final ModelParser modelParser = new ModelParser();
    private final ModelBinarySerializer binarySerializer = new ModelBinarySerializer();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Model");

    ModelResourceCache(ResourceResolver resolver, BinaryPayloadCipherRegistry cipherRegistry) {
        this.resolver = resolver;
        this.cipherRegistry = cipherRegistry != null ? cipherRegistry : BinaryPayloadCipherRegistry.withDefaults();
    }

    Model loadModel(String path) {
        String key = resolver.normalizePath(path);
        Model cached = modelCache.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceResolver.ResourceLookup lookup = resolver.lookup(path, ResourceResolver.ResourceType.MODEL);
        if (lookup.hasBinary()) {
            Model model = readBinaryModel(lookup.getBinaryPath(), key);
            if (model != null) {
                modelCache.put(key, model);
                return model;
            }
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

    private Model readBinaryModel(Path path, String key) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (isEncryptedWithoutReadyKey(bytes)) {
                SkycoreClientHandshake.requestHelloFromServer("key_pending_model");
                return null;
            }
            SkycoreBinaryArchive archive = BinaryResourceIO.read(bytes, cipherRegistry::resolve);
            if (archive.getHeader().getType() != BinaryResourceType.MODEL) {
                reporter.parseFailed(key, path, new IllegalStateException("Unexpected binary type " + archive.getHeader().getType()));
                return null;
            }
            BinaryDataReader reader = new BinaryDataReader(archive.getPayload());
            return binarySerializer.read(reader);
        } catch (GeneralSecurityException securityEx) {
            reporter.parseFailed(key, path, securityEx);
            SkycoreClientHandshake.requestHelloFromServer("decrypt_failed_model");
            return null;
        } catch (Exception ex) {
            reporter.parseFailed(key, path, ex);
            return null;
        }
    }

    private boolean isEncryptedWithoutReadyKey(byte[] bytes) {
        if (bytes == null || bytes.length < BinaryResourceHeader.HEADER_SIZE) {
            return false;
        }
        int flags = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        boolean encrypted = (flags & BinaryResourceFlags.ENCRYPTED) != 0
            && (flags & BinaryResourceFlags.ALGO_MASK) != BinaryResourceFlags.ALGO_NONE;
        return encrypted && !BinaryKeyManager.isKeyReady();
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
