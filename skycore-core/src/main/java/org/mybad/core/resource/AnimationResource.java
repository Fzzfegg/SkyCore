package org.mybad.core.resource;

import org.mybad.core.animation.*;
import org.mybad.core.parsing.*;

/**
 * 动画资源
 * 包装动画数据并支持加载和卸载
 */
public class AnimationResource implements Resource {
    private String resourceId;
    private Animation animation;
    private String jsonContent;
    private boolean loaded;

    public AnimationResource(String resourceId, String jsonContent) {
        this.resourceId = resourceId;
        this.jsonContent = jsonContent;
        this.animation = null;
        this.loaded = false;
    }

    public AnimationResource(String resourceId, Animation animation) {
        this.resourceId = resourceId;
        this.animation = animation;
        this.jsonContent = null;
        this.loaded = true;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String getResourceType() {
        return "Animation";
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void load() throws Exception {
        if (loaded) {
            return;
        }

        if (jsonContent == null) {
            throw new IllegalStateException("无法加载动画：JSON内容为空");
        }

        try {
            AnimationParser parser = new AnimationParser();
            AnimationParser.AnimationData animData = parser.parse(jsonContent);

            animation = new Animation(animData.name);
            animation.setLength(animData.length);
            animation.setLoopMode(animData.loopMode);
            animation.setOverridePreviousAnimation(animData.overridePreviousAnimation);

            for (String boneName : animData.boneAnimations.keySet()) {
                AnimationParser.BoneAnimation boneAnimData = animData.boneAnimations.get(boneName);
                Animation.BoneAnimation boneAnim = new Animation.BoneAnimation(boneName);

                for (AnimationParser.KeyFrame kf : boneAnimData.positionFrames) {
                    Animation.KeyFrame keyFrame = new Animation.KeyFrame(kf.timestamp, kf.value);
                    keyFrame.interpolation = InterpolationImpl.getInstance(kf.interpolation.name().toLowerCase().replace('_', '.'));
                    boneAnim.positionFrames.add(keyFrame);
                }

                for (AnimationParser.KeyFrame kf : boneAnimData.rotationFrames) {
                    Animation.KeyFrame keyFrame = new Animation.KeyFrame(kf.timestamp, kf.value);
                    keyFrame.interpolation = InterpolationImpl.getInstance(kf.interpolation.name().toLowerCase().replace('_', '.'));
                    boneAnim.rotationFrames.add(keyFrame);
                }

                for (AnimationParser.KeyFrame kf : boneAnimData.scaleFrames) {
                    Animation.KeyFrame keyFrame = new Animation.KeyFrame(kf.timestamp, kf.value);
                    keyFrame.interpolation = InterpolationImpl.getInstance(kf.interpolation.name().toLowerCase().replace('_', '.'));
                    boneAnim.scaleFrames.add(keyFrame);
                }

                animation.addBoneAnimation(boneName, boneAnim);
            }

            for (AnimationParser.AnimationEvent evt : animData.particleEvents) {
                if (evt != null) {
                    animation.addParticleEvent(evt.timestamp, evt.effect, evt.locator);
                }
            }
            for (AnimationParser.AnimationEvent evt : animData.soundEvents) {
                if (evt != null) {
                    animation.addSoundEvent(evt.timestamp, evt.effect, evt.locator);
                }
            }

            loaded = true;
        } catch (org.mybad.core.exception.ParseException e) {
            throw new Exception("动画解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unload() {
        animation = null;
        jsonContent = null;
        loaded = false;
    }

    @Override
    public long getSize() {
        if (animation == null) {
            return jsonContent != null ? jsonContent.length() : 0;
        }

        // 估算动画大小
        long size = 100;  // 基础对象开销
        for (Animation.BoneAnimation boneAnim : animation.getBoneAnimations().values()) {
            size += 100;  // 每个骨骼动画的基础开销
            size += boneAnim.positionFrames.size() * 48;      // float[3] = 12字节 + 开销
            size += boneAnim.rotationFrames.size() * 48;
            size += boneAnim.scaleFrames.size() * 48;
        }
        return size;
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    /**
     * 获取动画
     */
    public Animation getAnimation() {
        return animation;
    }
}
