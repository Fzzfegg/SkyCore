package org.mybad.minecraft.resource;

import org.mybad.core.animation.Animation;
import org.mybad.core.parsing.AnimationParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AnimationResourceCache {
    private final ResourceLoader owner;
    private final Map<String, Animation> animationCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Animation>> animationSetCache = new ConcurrentHashMap<>();
    private final AnimationParser animationParser = new AnimationParser();
    private final ResourceLoadReporter reporter = new ResourceLoadReporter("Animation");

    AnimationResourceCache(ResourceLoader owner) {
        this.owner = owner;
    }

    Animation loadAnimation(String path) {
        Animation cached = animationCache.get(path);
        if (cached != null) {
            return cached;
        }
        Animation animation = loadAnimation(path, null);
        if (animation != null) {
            animationCache.put(path, animation);
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
        Map<String, Animation> cached = animationSetCache.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            String jsonContent = owner.readResourceAsString(path);
            if (jsonContent == null) {
                reporter.missing(path);
                return null;
            }
            Map<String, Animation> animations = animationParser.parseAllToAnimations(jsonContent);
            animationSetCache.put(path, animations);
            return animations;
        } catch (Exception e) {
            reporter.parseFailed(path, e);
            return null;
        }
    }

    void invalidateAnimation(String path) {
        animationCache.remove(path);
        animationSetCache.remove(path);
    }

    int getCachedAnimationCount() {
        return animationCache.size();
    }

    void clear() {
        animationCache.clear();
        animationSetCache.clear();
    }
}
