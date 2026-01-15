package org.mybad.minecraft.render.trail;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser;
import org.mybad.minecraft.render.entity.events.AnimationEventMathUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages multiple weapon trail clips for a single entity.
 */
public final class WeaponTrailController {
    private final Map<String, WeaponTrailClip> clips = new HashMap<>();
    private long lastUpdateNanos = 0L;

    public void handle(AnimationEventArgsParser.TrailParams params) {
        if (params == null || params.id == null || params.id.isEmpty()) {
            debug("忽略 trail 事件: 缺少 id", params);
            return;
        }
        if (params.action == TrailAction.STOP) {
            WeaponTrailClip clip = clips.get(params.id);
            if (clip != null) {
                clip.stopEmission();
                debug("停止 trail", params);
            } else {
                debug("停止 trail 但未找到 clip", params);
            }
            return;
        }
        boolean hasLocatorStart = params.locatorStart != null && !params.locatorStart.isEmpty();
        boolean hasLocatorEnd = params.locatorEnd != null && !params.locatorEnd.isEmpty();
        boolean hasStart = hasLocatorStart || params.offsetStart != null;
        boolean hasEnd = hasLocatorEnd || params.offsetEnd != null;
        if (!hasStart && !hasEnd) {
            debug("忽略 trail 事件: 既没有 locator 也没有 offset", params);
            return;
        }
        WeaponTrailClip clip = clips.get(params.id);
        if (clip == null) {
            clip = new WeaponTrailClip(params);
            clips.put(params.id, clip);
            debug("创建 trail", params);
        } else {
            clip.applyParams(params);
            debug("更新 trail 参数", params);
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

    private void debug(String msg, AnimationEventArgsParser.TrailParams params) {
        SkyCoreMod.LOGGER.info("[SkyTrail] {} -> id={}, locatorA={}, locatorB={}, texture={}, action={}, raw={}, offsetA={}, offsetB={}, width={}, axis={}",
            msg,
            params != null ? params.id : "null",
            params != null ? params.locatorStart : "null",
            params != null ? params.locatorEnd : "null",
            params != null && params.texture != null ? params.texture : "null",
            params != null ? params.action : "null",
            params != null ? params.rawEffect : "null",
            params != null && params.offsetStart != null ? java.util.Arrays.toString(params.offsetStart) : "null",
            params != null && params.offsetEnd != null ? java.util.Arrays.toString(params.offsetEnd) : "null",
            params != null ? params.width : 0f,
            params != null && params.axis != null ? params.axis : TrailAxis.Z);
    }
}
