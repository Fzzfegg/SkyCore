package org.mybad.core.parsing;

import com.google.gson.*;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.InterpolationImpl;
import org.mybad.core.exception.ParseException;

import java.util.*;

/**
 * 基岩格式动画解析器
 * 负责解析动画数据结构
 *
 * 支持的关键帧格式：
 * 1. 简单数值：1.5
 * 2. 数组：[1.0, 2.0, 3.0]
 * 3. 带插值的对象：{"post": [...], "pre": [...], "lerp_mode": "linear"}
 * 4. easing曲线
 */
public class AnimationParser {

    /**
     * 支持的插值模式（27种）
     */
    public enum InterpolationMode {
        LINEAR("linear"),
        CATMULLROM("catmullrom"),
        STEP("step"),
        BEZIER("bezier"),
        QUAD_IN("quad.in"),
        QUAD_OUT("quad.out"),
        QUAD_INOUT("quad.inout"),
        CUBIC_IN("cubic.in"),
        CUBIC_OUT("cubic.out"),
        CUBIC_INOUT("cubic.inout"),
        QUART_IN("quart.in"),
        QUART_OUT("quart.out"),
        QUART_INOUT("quart.inout"),
        QUINT_IN("quint.in"),
        QUINT_OUT("quint.out"),
        QUINT_INOUT("quint.inout"),
        SINE_IN("sine.in"),
        SINE_OUT("sine.out"),
        SINE_INOUT("sine.inout"),
        EXPO_IN("expo.in"),
        EXPO_OUT("expo.out"),
        EXPO_INOUT("expo.inout"),
        CIRC_IN("circ.in"),
        CIRC_OUT("circ.out"),
        CIRC_INOUT("circ.inout"),
        BACK_IN("back.in"),
        BACK_OUT("back.out"),
        BACK_INOUT("back.inout"),
        ELASTIC_IN("elastic.in"),
        ELASTIC_OUT("elastic.out");

        private String name;

        InterpolationMode(String name) {
            this.name = name;
        }

        public static InterpolationMode fromString(String name) {
            if (name == null) {
                return LINEAR;
            }
            String normalized = name.trim().toLowerCase();
            if ("catmull_rom".equals(normalized)) {
                normalized = "catmullrom";
            }
            for (InterpolationMode mode : values()) {
                if (mode.name.equals(normalized)) {
                    return mode;
                }
            }
            return LINEAR;
        }
    }

    /**
     * 动画数据容器
     */
    public static class AnimationData {
        public String name;
        public float length;
        public Animation.LoopMode loopMode;
        public boolean overridePreviousAnimation;
        public Map<String, BoneAnimation> boneAnimations;
        public List<AnimationEvent> particleEvents;
        public List<AnimationEvent> soundEvents;

        public AnimationData(String name) {
            this.name = name;
            this.length = 0;
            this.loopMode = Animation.LoopMode.ONCE;
            this.overridePreviousAnimation = false;
            this.boneAnimations = new HashMap<>();
            this.particleEvents = new ArrayList<>();
            this.soundEvents = new ArrayList<>();
        }
    }

    /**
     * 动画事件数据
     */
    public static class AnimationEvent {
        public float timestamp;
        public String effect;
        public String locator;

        public AnimationEvent(float timestamp, String effect, String locator) {
            this.timestamp = timestamp;
            this.effect = effect;
            this.locator = locator;
        }
    }

    /**
     * 单个骨骼的动画数据
     */
    public static class BoneAnimation {
        public String boneName;
        public List<KeyFrame> positionFrames;  // 位置关键帧
        public List<KeyFrame> rotationFrames;  // 旋转关键帧
        public List<KeyFrame> scaleFrames;     // 缩放关键帧

        public BoneAnimation(String boneName) {
            this.boneName = boneName;
            this.positionFrames = new ArrayList<>();
            this.rotationFrames = new ArrayList<>();
            this.scaleFrames = new ArrayList<>();
        }
    }

    /**
     * 关键帧数据
     */
    public static class KeyFrame {
        public float timestamp;
        public float[] value;               // 值 [x, y, z] 或单个值
        public InterpolationMode interpolation;
        public float[] post;                // 出切线（用于hermite插值）
        public float[] pre;                 // 入切线

        public KeyFrame(float timestamp, float[] value) {
            this.timestamp = timestamp;
            this.value = value;
            this.interpolation = InterpolationMode.LINEAR;
        }
    }

    /**
     * 从JSON字符串解析动画
     */
    public AnimationData parse(String jsonContent) throws ParseException {
        try {
            JsonElement element = new JsonParser().parse(jsonContent);
            if (!element.isJsonObject()) {
                throw new ParseException("JSON必须是对象格式");
            }
            return parseJsonObject(element.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            throw new ParseException("动画JSON语法错误: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON字符串解析所有动画
     */
    public Map<String, AnimationData> parseAll(String jsonContent) throws ParseException {
        try {
            JsonElement element = new JsonParser().parse(jsonContent);
            if (!element.isJsonObject()) {
                throw new ParseException("JSON必须是对象格式");
            }
            return parseAllJsonObject(element.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            throw new ParseException("动画JSON语法错误: " + e.getMessage(), e);
        }
    }

    /**
     * 从JsonObject解析动画
     */
    public AnimationData parseJsonObject(JsonObject root) throws ParseException {
        // 通常动画数据在 "animations" 对象中，或者直接是动画定义
        JsonObject animationsObj = ParseUtils.getObject(root, "animations");

        if (animationsObj.entrySet().isEmpty()) {
            throw new ParseException("未找到动画数据");
        }

        // 获取第一个动画（通常一个JSON文件一个动画）
        Map.Entry<String, JsonElement> firstEntry = animationsObj.entrySet().iterator().next();
        String animationName = firstEntry.getKey();
        JsonObject animationJson = firstEntry.getValue().getAsJsonObject();

        return parseAnimation(animationName, animationJson);
    }

    public Map<String, AnimationData> parseAllJsonObject(JsonObject root) throws ParseException {
        JsonObject animationsObj = ParseUtils.getObject(root, "animations");
        if (animationsObj.entrySet().isEmpty()) {
            throw new ParseException("未找到动画数据");
        }
        Map<String, AnimationData> results = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            String animationName = entry.getKey();
            JsonObject animationJson = entry.getValue().getAsJsonObject();
            AnimationData data = parseAnimation(animationName, animationJson);
            results.put(animationName, data);
        }
        return results;
    }

    /**
     * 解析单个动画
     */
    public AnimationData parseAnimation(String name, JsonObject animJson) throws ParseException {
        AnimationData animation = new AnimationData(name);

        // 解析长度
        if (animJson.has("animation_length")) {
            animation.length = ParseUtils.getFloat(animJson, "animation_length", 1.0f);
        } else {
            animation.length = ParseUtils.getFloat(animJson, "length", 1.0f);
        }

        // 解析循环设置与覆盖设置
        animation.loopMode = parseLoopMode(animJson);
        animation.overridePreviousAnimation = ParseUtils.getBoolean(animJson, "override_previous_animation", false);

        // 解析骨骼动画
        JsonObject bonesObj = ParseUtils.getObject(animJson, "bones");
        for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
            String boneName = entry.getKey();
            JsonObject boneAnimJson = entry.getValue().getAsJsonObject();
            BoneAnimation boneAnim = parseBoneAnimation(boneName, boneAnimJson);
            animation.boneAnimations.put(boneName, boneAnim);

            // 更新动画长度
            float boneMaxTime = getMaxKeyframeTime(boneAnim);
            animation.length = Math.max(animation.length, boneMaxTime);
        }

        // 解析动画事件
        parseEventMap(animation.particleEvents, animJson, "particle_effects");
        parseEventMap(animation.soundEvents, animJson, "sound_effects");

        // 事件可能延长动画长度
        float eventMax = getMaxEventTime(animation);
        animation.length = Math.max(animation.length, eventMax);

        return animation;
    }

    /**
     * 解析单个骨骼的动画
     */
    private BoneAnimation parseBoneAnimation(String boneName, JsonObject boneJson) throws ParseException {
        BoneAnimation boneAnim = new BoneAnimation(boneName);

        parseChannel(boneAnim.positionFrames, boneJson, "position");
        parseChannel(boneAnim.rotationFrames, boneJson, "rotation");
        parseChannel(boneAnim.scaleFrames, boneJson, "scale");

        // 按时间排序关键帧
        Collections.sort(boneAnim.positionFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));
        Collections.sort(boneAnim.rotationFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));
        Collections.sort(boneAnim.scaleFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));

        return boneAnim;
    }

    private void parseChannel(List<KeyFrame> frames, JsonObject boneJson, String key) {
        if (!boneJson.has(key)) {
            return;
        }

        JsonElement channel = boneJson.get(key);
        if (channel.isJsonObject()) {
            JsonObject obj = channel.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                float time = parseTime(entry.getKey());
                KeyFrame frame = parseKeyFrame(time, entry.getValue());
                frames.add(frame);
            }
        } else {
            // 直接值（数组/数值），视为 time=0 的关键帧
            KeyFrame frame = parseKeyFrame(0f, channel);
            frames.add(frame);
        }
    }

    /**
     * 解析时间字符串
     * 格式：时间戳（秒）
     */
    private float parseTime(String timeStr) {
        try {
            return Float.parseFloat(timeStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析关键帧
     * 支持多种格式：
     * - [1.0, 2.0, 3.0] 简单数组
     * - {"post": [...], "pre": [...], "lerp_mode": "..."}  带easing的对象
     */
    private KeyFrame parseKeyFrame(float timestamp, JsonElement valueElement) {
        float[] value = new float[3];
        InterpolationMode interpolation = InterpolationMode.LINEAR;
        float[] post = null;
        float[] pre = null;

        if (valueElement.isJsonArray()) {
            // 简单格式
            value = normalizeVec3(ParseUtils.parseFloatArray(valueElement, 0, 0, 0));
        } else if (valueElement.isJsonPrimitive()) {
            float v = valueElement.getAsFloat();
            value = new float[]{v, v, v};
        } else if (valueElement.isJsonObject()) {
            // 复杂格式
            JsonObject obj = valueElement.getAsJsonObject();

            // 解析插值模式
            String lerpMode = ParseUtils.getString(obj, "lerp_mode", "linear");
            interpolation = InterpolationMode.fromString(lerpMode);

            // 解析值
            if (obj.has("post")) {
                value = normalizeVec3(ParseUtils.parseFloatArray(obj.get("post"), 0, 0, 0));
            } else if (obj.has("pre")) {
                value = normalizeVec3(ParseUtils.parseFloatArray(obj.get("pre"), 0, 0, 0));
            }

            // 解析easing切线
            if (obj.has("post")) {
                post = normalizeVec3(ParseUtils.parseFloatArray(obj.get("post"), 0, 0, 0));
            }
            if (obj.has("pre")) {
                pre = normalizeVec3(ParseUtils.parseFloatArray(obj.get("pre"), 0, 0, 0));
            }
        }

        KeyFrame frame = new KeyFrame(timestamp, value);
        frame.interpolation = interpolation;
        frame.post = post;
        frame.pre = pre;

        return frame;
    }

    private float[] normalizeVec3(float[] raw) {
        if (raw == null || raw.length == 0) {
            return new float[]{0, 0, 0};
        }
        if (raw.length == 1) {
            return new float[]{raw[0], raw[0], raw[0]};
        }
        if (raw.length == 2) {
            return new float[]{raw[0], raw[1], 0};
        }
        if (raw.length == 3) {
            return raw;
        }
        return new float[]{raw[0], raw[1], raw[2]};
    }

    /**
     * 获取骨骼动画中的最大关键帧时间
     */
    private float getMaxKeyframeTime(BoneAnimation boneAnim) {
        float max = 0;
        for (KeyFrame frame : boneAnim.positionFrames) {
            max = Math.max(max, frame.timestamp);
        }
        for (KeyFrame frame : boneAnim.rotationFrames) {
            max = Math.max(max, frame.timestamp);
        }
        for (KeyFrame frame : boneAnim.scaleFrames) {
            max = Math.max(max, frame.timestamp);
        }
        return max;
    }

    private void parseEventMap(List<AnimationEvent> target, JsonObject animJson, String key) {
        if (animJson == null || !animJson.has(key)) {
            return;
        }
        JsonElement element = animJson.get(key);
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject obj = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            float time = parseTime(entry.getKey());
            JsonElement value = entry.getValue();
            String effect = null;
            String locator = null;
            if (value.isJsonObject()) {
                JsonObject data = value.getAsJsonObject();
                effect = ParseUtils.getString(data, "effect", null);
                locator = ParseUtils.getString(data, "locator", null);
            } else if (value.isJsonPrimitive()) {
                effect = value.getAsString();
            }
            if (effect == null || effect.trim().isEmpty()) {
                continue;
            }
            target.add(new AnimationEvent(time, effect.trim(), locator));
        }
        target.sort(Comparator.comparingDouble(e -> e.timestamp));
    }

    private float getMaxEventTime(AnimationData animation) {
        float max = 0f;
        for (AnimationEvent evt : animation.particleEvents) {
            if (evt != null) {
                max = Math.max(max, evt.timestamp);
            }
        }
        for (AnimationEvent evt : animation.soundEvents) {
            if (evt != null) {
                max = Math.max(max, evt.timestamp);
            }
        }
        return max;
    }

    /**
     * 将 AnimationData 转换为 Animation
     * 用于将解析后的数据转换为运行时动画对象
     */
    public Animation toAnimation(AnimationData data) {
        Animation animation = new Animation(data.name, data.length);
        animation.setLoopMode(data.loopMode);
        animation.setOverridePreviousAnimation(data.overridePreviousAnimation);

        for (Map.Entry<String, BoneAnimation> entry : data.boneAnimations.entrySet()) {
            String boneName = entry.getKey();
            BoneAnimation srcBoneAnim = entry.getValue();

            Animation.BoneAnimation dstBoneAnim = new Animation.BoneAnimation(boneName);

            // 转换位置关键帧
            for (KeyFrame srcFrame : srcBoneAnim.positionFrames) {
                Animation.KeyFrame dstFrame = convertKeyFrame(srcFrame);
                dstBoneAnim.positionFrames.add(dstFrame);
            }

            // 转换旋转关键帧
            for (KeyFrame srcFrame : srcBoneAnim.rotationFrames) {
                Animation.KeyFrame dstFrame = convertKeyFrame(srcFrame);
                dstBoneAnim.rotationFrames.add(dstFrame);
            }

            // 转换缩放关键帧
            for (KeyFrame srcFrame : srcBoneAnim.scaleFrames) {
                Animation.KeyFrame dstFrame = convertKeyFrame(srcFrame);
                dstBoneAnim.scaleFrames.add(dstFrame);
            }

            animation.addBoneAnimation(boneName, dstBoneAnim);
        }

        for (AnimationEvent evt : data.particleEvents) {
            if (evt != null) {
                animation.addParticleEvent(evt.timestamp, evt.effect, evt.locator);
            }
        }
        for (AnimationEvent evt : data.soundEvents) {
            if (evt != null) {
                animation.addSoundEvent(evt.timestamp, evt.effect, evt.locator);
            }
        }

        return animation;
    }

    public Map<String, Animation> toAnimations(Map<String, AnimationData> dataMap) {
        Map<String, Animation> animations = new LinkedHashMap<>();
        for (Map.Entry<String, AnimationData> entry : dataMap.entrySet()) {
            animations.put(entry.getKey(), toAnimation(entry.getValue()));
        }
        return animations;
    }

    /**
     * 转换单个关键帧
     */
    private Animation.KeyFrame convertKeyFrame(KeyFrame src) {
        String interpMode = src.interpolation != null ? src.interpolation.name : "linear";
        Animation.KeyFrame dst = new Animation.KeyFrame(src.timestamp, src.value, interpMode);
        dst.post = src.post;
        dst.pre = src.pre;
        return dst;
    }

    private Animation.LoopMode parseLoopMode(JsonObject animJson) {
        if (!animJson.has("loop")) {
            return Animation.LoopMode.ONCE;
        }
        JsonElement elem = animJson.get("loop");
        if (elem != null && elem.isJsonPrimitive()) {
            JsonPrimitive primitive = elem.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? Animation.LoopMode.LOOP : Animation.LoopMode.ONCE;
            }
            if (primitive.isString()) {
                String mode = primitive.getAsString().trim();
                if ("hold_on_last_frame".equalsIgnoreCase(mode)) {
                    return Animation.LoopMode.HOLD_ON_LAST_FRAME;
                }
                if ("loop".equalsIgnoreCase(mode) || "true".equalsIgnoreCase(mode)) {
                    return Animation.LoopMode.LOOP;
                }
                if ("once".equalsIgnoreCase(mode) || "false".equalsIgnoreCase(mode)) {
                    return Animation.LoopMode.ONCE;
                }
            }
        }
        return Animation.LoopMode.ONCE;
    }

    /**
     * 便捷方法：解析并直接返回 Animation 对象
     */
    public Animation parseToAnimation(String jsonContent) throws ParseException {
        AnimationData data = parse(jsonContent);
        return toAnimation(data);
    }

    public Map<String, Animation> parseAllToAnimations(String jsonContent) throws ParseException {
        Map<String, AnimationData> data = parseAll(jsonContent);
        return toAnimations(data);
    }
}
