package org.mybad.minecraft.render.trail;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser;
import org.mybad.minecraft.render.entity.events.AnimationEventMathUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;


public final class WeaponTrailController {
    private final Map<String, WeaponTrailClip> clips = new HashMap<>();
    private long lastUpdateNanos = 0L;

    public void handle(AnimationEventArgsParser.TrailParams params) {
        if (params == null || params.id == null || params.id.isEmpty()) {return;}
        if (params.action == TrailAction.STOP) {
            WeaponTrailClip clip = clips.get(params.id);
            if (clip != null) {
                clip.stopEmission();
            }
            return;
        }
        boolean hasLocator = params.locatorStart != null && !params.locatorStart.isEmpty();
        if (!hasLocator) {
            return;
        }
        WeaponTrailClip clip = clips.get(params.id);
        if (clip == null) {
            clip = new WeaponTrailClip(params);
            clips.put(params.id, clip);
        } else {
            clip.applyParams(params);
        }
        clip.startEmission();
    }

    public void update(EntityLivingBase entity, BedrockModelHandle wrapper, float partialTicks) {
        if (clips.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        float deltaSeconds;
        if (lastUpdateNanos == 0L) {
            deltaSeconds = 0f;
        } else {
            deltaSeconds = (now - lastUpdateNanos) / 1_000_000_000f;
            if (deltaSeconds > 0.1f) {
                deltaSeconds = 0.1f;
            } else if (deltaSeconds < 0f) {
                deltaSeconds = 0f;
            }
        }
        lastUpdateNanos = now;
        float headYaw = entity != null ? AnimationEventMathUtil.resolveHeadYaw(entity, partialTicks) : 0f;
        Iterator<Map.Entry<String, WeaponTrailClip>> iterator = clips.entrySet().iterator();
        while (iterator.hasNext()) {
            WeaponTrailClip clip = iterator.next().getValue();
            clip.update(entity, wrapper, partialTicks, deltaSeconds, headYaw);
            if (!clip.isAlive()) {
                iterator.remove();
            }
        }
    }

    public void forEachRenderable(Consumer<WeaponTrailClip> consumer) {
        if (consumer == null || clips.isEmpty()) {
            return;
        }
        for (WeaponTrailClip clip : clips.values()) {
            if (clip.hasRenderableGeometry()) {
                consumer.accept(clip);
            }
        }
    }

    public void clear() {
        clips.clear();
        lastUpdateNanos = 0L;
    }

    public boolean isEmpty() {
        return clips.isEmpty();
    }


}
