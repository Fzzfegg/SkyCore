package org.mybad.minecraft.resource;

import org.mybad.core.animation.Animation;
import org.mybad.core.parsing.AnimationParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AnimationResourceCache {
    private final ResourceResolver resolver;
    private final Map<String, Animation> animationCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Animation>> animationSetCache = new ConcurrentHashMap<>();
    private final AnimationParser animationParser = new AnimationParser();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Animation");

    AnimationResourceCache(ResourceResolver resolver) {
        this.resolver = resolver;
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
