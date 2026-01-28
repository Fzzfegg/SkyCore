package org.mybad.minecraft.resource;

import org.mybad.core.animation.Animation;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;
import org.mybad.core.binary.BinaryResourceIO;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.binary.SkycoreBinaryArchive;
import org.mybad.core.binary.animation.AnimationSetBinarySerializer;
import org.mybad.core.parsing.AnimationParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AnimationResourceCache {
    private final ResourceResolver resolver;
    private final BinaryPayloadCipherRegistry cipherRegistry;
    private final Map<String, Animation> animationCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Animation>> animationSetCache = new ConcurrentHashMap<>();
    private final AnimationParser animationParser = new AnimationParser();
    private final AnimationSetBinarySerializer animationSetSerializer = new AnimationSetBinarySerializer();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Animation");

    AnimationResourceCache(ResourceResolver resolver, BinaryPayloadCipherRegistry cipherRegistry) {
        this.resolver = resolver;
        this.cipherRegistry = cipherRegistry != null ? cipherRegistry : BinaryPayloadCipherRegistry.withDefaults();
    }

    Animation loadAnimation(String path) {
        String key = resolver.normalizePath(path);
        Animation cached = animationCache.get(key);
        if (cached != null) {
            return cached;
        }
        Animation animation = loadAnimation(path, null);
        if (animation != null) {
            animationCache.put(key, animation);
        }
        return animation;
    }

    Animation loadAnimation(String path, String clipName) {
        Map<String, Animation> set = loadAnimationSet(path);
        if (set == null || set.isEmpty()) {
            return null;
        }
        if (clipName == null || clipName.trim().isEmpty()) {
            return set.values().iterator().next();
        }
        Animation animation = set.get(clipName);
        if (animation == null) {
            reporter.warn("动画片段不存在: {} in {}", clipName, path);
        }
        return animation;
    }

    Map<String, Animation> loadAnimationSet(String path) {
        String key = resolver.normalizePath(path);
        Map<String, Animation> cached = animationSetCache.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceResolver.ResourceLookup lookup = resolver.lookup(path, ResourceResolver.ResourceType.ANIMATION);
        if (lookup.hasBinary()) {
            Map<String, Animation> animations = readBinaryAnimations(lookup.getBinaryPath(), key);
            if (animations != null) {
                animationSetCache.put(key, animations);
                return animations;
            }
        }
        try {
            String jsonContent = resolver.readResourceAsString(key);
            if (jsonContent == null) {
                reporter.missing(key);
                return null;
            }
            Map<String, Animation> animations = animationParser.parseAllToAnimations(jsonContent);
            animationSetCache.put(key, animations);
            return animations;
        } catch (Exception e) {
            reporter.parseFailed(key, e);
            return null;
        }
    }

    private Map<String, Animation> readBinaryAnimations(Path path, String key) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            SkycoreBinaryArchive archive = BinaryResourceIO.read(bytes, cipherRegistry::resolve);
            if (archive.getHeader().getType() != BinaryResourceType.ANIMATION) {
                reporter.parseFailed(key, path, new IllegalStateException("Unexpected binary type " + archive.getHeader().getType()));
                return null;
            }
            BinaryDataReader reader = new BinaryDataReader(archive.getPayload());
            return animationSetSerializer.read(reader);
        } catch (Exception ex) {
            reporter.parseFailed(key, path, ex);
            return null;
        }
    }

    void invalidateAnimation(String path) {
        String key = resolver.normalizePath(path);
        animationCache.remove(key);
        animationSetCache.remove(key);
    }

    int getCachedAnimationCount() {
        return animationCache.size();
    }

    void clear() {
        animationCache.clear();
        animationSetCache.clear();
    }
}
