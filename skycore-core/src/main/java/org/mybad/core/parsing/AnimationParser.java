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
            for (InterpolationMode mode : values()) {
                if (mode.name.equals(name)) {
                    return mode;
                }
            }
            return LINEAR;  // 默认
        }
    }

    /**
     * 动画数据容器
     */
    public static class AnimationData {
        public String name;
        public float length;
        public boolean loop;
        public Map<String, BoneAnimation> boneAnimations;

        public AnimationData(String name) {
            this.name = name;
            this.length = 0;
            this.loop = false;
            this.boneAnimations = new HashMap<>();
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

    /**
     * 解析单个动画
     */
    public AnimationData parseAnimation(String name, JsonObject animJson) throws ParseException {
        AnimationData animation = new AnimationData(name);

        // 解析长度
        animation.length = ParseUtils.getFloat(animJson, "length", 1.0f);

        // 解析循环设置
        animation.loop = ParseUtils.getBoolean(animJson, "loop", false);

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

        return animation;
    }

    /**
     * 解析单个骨骼的动画
     */
    private BoneAnimation parseBoneAnimation(String boneName, JsonObject boneJson) throws ParseException {
        BoneAnimation boneAnim = new BoneAnimation(boneName);

        // 解析位置关键帧
        JsonObject positionObj = ParseUtils.getObject(boneJson, "position");
        for (Map.Entry<String, JsonElement> entry : positionObj.entrySet()) {
            float time = parseTime(entry.getKey());
            KeyFrame frame = parseKeyFrame(time, entry.getValue());
            boneAnim.positionFrames.add(frame);
        }

        // 解析旋转关键帧
        JsonObject rotationObj = ParseUtils.getObject(boneJson, "rotation");
        for (Map.Entry<String, JsonElement> entry : rotationObj.entrySet()) {
            float time = parseTime(entry.getKey());
            KeyFrame frame = parseKeyFrame(time, entry.getValue());
            boneAnim.rotationFrames.add(frame);
        }

        // 解析缩放关键帧
        JsonObject scaleObj = ParseUtils.getObject(boneJson, "scale");
        for (Map.Entry<String, JsonElement> entry : scaleObj.entrySet()) {
            float time = parseTime(entry.getKey());
            KeyFrame frame = parseKeyFrame(time, entry.getValue());
            boneAnim.scaleFrames.add(frame);
        }

        // 按时间排序关键帧
        Collections.sort(boneAnim.positionFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));
        Collections.sort(boneAnim.rotationFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));
        Collections.sort(boneAnim.scaleFrames, (a, b) -> Float.compare(a.timestamp, b.timestamp));

        return boneAnim;
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
            value = ParseUtils.parseFloatArray(valueElement, 0, 0, 0);
        } else if (valueElement.isJsonObject()) {
            // 复杂格式
            JsonObject obj = valueElement.getAsJsonObject();

            // 解析插值模式
            String lerpMode = ParseUtils.getString(obj, "lerp_mode", "linear");
            interpolation = InterpolationMode.fromString(lerpMode);

            // 解析值
            if (obj.has("post")) {
                value = ParseUtils.parseFloatArray(obj.get("post"), 0, 0, 0);
            }

            // 解析easing切线
            if (obj.has("post")) {
                post = ParseUtils.parseFloatArray(obj.get("post"), 0, 0, 0);
            }
            if (obj.has("pre")) {
                pre = ParseUtils.parseFloatArray(obj.get("pre"), 0, 0, 0);
            }
        }

        KeyFrame frame = new KeyFrame(timestamp, value);
        frame.interpolation = interpolation;
        frame.post = post;
        frame.pre = pre;

        return frame;
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

    /**
     * 将 AnimationData 转换为 Animation
     * 用于将解析后的数据转换为运行时动画对象
     */
    public Animation toAnimation(AnimationData data) {
        Animation animation = new Animation(data.name, data.length);
        animation.setLoop(data.loop);

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

        return animation;
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

    /**
     * 便捷方法：解析并直接返回 Animation 对象
     */
    public Animation parseToAnimation(String jsonContent) throws ParseException {
        AnimationData data = parse(jsonContent);
        return toAnimation(data);
    }
}
