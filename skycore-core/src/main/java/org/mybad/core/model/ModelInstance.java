package org.mybad.core.model;

import org.mybad.core.data.ModelBone;
import org.mybad.core.animation.AnimationState;
import org.mybad.core.animation.Animation;
import java.util.*;

/**
 * 模型实例 - 模型的运行时实例
 * 基于 ModelDefinition 创建，存储实例特定的状态（变换、动画、属性等）
 * 多个实例可以共享同一个 ModelDefinition，节省内存
 */
public class ModelInstance {

    private String instanceId;
    private ModelDefinition definition;

    // 实例特定状态
    private Map<String, float[]> boneTransforms;    // 骨骼变换（位置、旋转、缩放）
    private Map<String, AnimationState> animationStates;  // 动画播放状态
    private Map<String, Object> instanceProperties; // 实例属性

    // 性能优化
    private boolean transformDirty;
    private long lastUpdateTime;

    // 事件和回调
    private List<InstanceEventListener> eventListeners;

    public ModelInstance(String instanceId, ModelDefinition definition) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.boneTransforms = new HashMap<>();
        this.animationStates = new HashMap<>();
        this.instanceProperties = new HashMap<>();
        this.transformDirty = true;
        this.lastUpdateTime = System.currentTimeMillis();
        this.eventListeners = new ArrayList<>();

        // 初始化骨骼变换
        initializeBoneTransforms();
    }

    /**
     * 初始化骨骼变换矩阵
     */
    private void initializeBoneTransforms() {
        for (ModelBone bone : definition.getBaseModel().getBones()) {
            initializeBoneTransform(bone);
        }
    }

    /**
     * 初始化单个骨骼的变换
     */
    private void initializeBoneTransform(ModelBone bone) {
        // 每个骨骼的变换为：[x, y, z, rotX, rotY, rotZ, scaleX, scaleY, scaleZ]
        float[] transform = new float[9];

        // 位置
        transform[0] = bone.getPivotX();
        transform[1] = bone.getPivotY();
        transform[2] = bone.getPivotZ();

        // 旋转
        transform[3] = 0f;
        transform[4] = 0f;
        transform[5] = 0f;

        // 缩放
        transform[6] = 1f;
        transform[7] = 1f;
        transform[8] = 1f;

        boneTransforms.put(bone.getName(), transform);

        // 递归初始化子骨骼
        for (ModelBone child : bone.getChildren()) {
            initializeBoneTransform(child);
        }
    }

    /**
     * 获取骨骼变换
     * @param boneName 骨骼名称
     * @return 变换数组 [x, y, z, rotX, rotY, rotZ, scaleX, scaleY, scaleZ]
     */
    public float[] getBoneTransform(String boneName) {
        return boneTransforms.get(boneName);
    }

    /**
     * 设置骨骼位置
     */
    public void setBonePosition(String boneName, float x, float y, float z) {
        float[] transform = boneTransforms.get(boneName);
        if (transform != null) {
            transform[0] = x;
            transform[1] = y;
            transform[2] = z;
            transformDirty = true;
        }
    }

    /**
     * 设置骨骼旋转
     */
    public void setBoneRotation(String boneName, float rotX, float rotY, float rotZ) {
        float[] transform = boneTransforms.get(boneName);
        if (transform != null) {
            transform[3] = rotX;
            transform[4] = rotY;
            transform[5] = rotZ;
            transformDirty = true;
        }
    }

    /**
     * 设置骨骼缩放
     */
    public void setBoneScale(String boneName, float scaleX, float scaleY, float scaleZ) {
        float[] transform = boneTransforms.get(boneName);
        if (transform != null) {
            transform[6] = scaleX;
            transform[7] = scaleY;
            transform[8] = scaleZ;
            transformDirty = true;
        }
    }

    /**
     * 播放动画
     */
    public void playAnimation(String animationName, float speed) throws AnimationException {
        // 这里需要从资源管理系统获取动画
        // 为了简化，直接创建动画状态
        Animation animation = new Animation(animationName, 1.0f);
        animation.setSpeed(speed);

        AnimationState state = new AnimationState(animation);
        state.play();

        animationStates.put(animationName, state);
        fireAnimationEvent("play", animationName);
    }

    /**
     * 停止动画
     */
    public void stopAnimation(String animationName) {
        AnimationState state = animationStates.get(animationName);
        if (state != null) {
            state.stop();
            fireAnimationEvent("stop", animationName);
        }
    }

    /**
     * 暂停动画
     */
    public void pauseAnimation(String animationName) {
        AnimationState state = animationStates.get(animationName);
        if (state != null) {
            state.pause();
        }
    }

    /**
     * 继续动画
     */
    public void resumeAnimation(String animationName) {
        AnimationState state = animationStates.get(animationName);
        if (state != null) {
            state.resume();
        }
    }

    /**
     * 获取动画状态
     */
    public AnimationState getAnimationState(String animationName) {
        return animationStates.get(animationName);
    }

    /**
     * 更新实例（处理动画、变换等）
     */
    public void update(float deltaTime) {
        lastUpdateTime = System.currentTimeMillis();

        // 更新所有播放中的动画
        for (AnimationState state : animationStates.values()) {
            state.update(deltaTime);
        }

        // 如果有变换更新，计算骨骼矩阵
        if (transformDirty) {
            // 这里会在实际渲染时调用矩阵计算
            transformDirty = false;
        }
    }

    /**
     * 设置实例属性
     */
    public void setProperty(String key, Object value) {
        instanceProperties.put(key, value);
    }

    /**
     * 获取实例属性
     */
    public Object getProperty(String key) {
        return instanceProperties.get(key);
    }

    /**
     * 检查属性是否存在
     */
    public boolean hasProperty(String key) {
        return instanceProperties.containsKey(key);
    }

    /**
     * 添加事件监听器
     */
    public void addEventListener(InstanceEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * 移除事件监听器
     */
    public void removeEventListener(InstanceEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * 触发动画事件
     */
    private void fireAnimationEvent(String eventType, String animationName) {
        for (InstanceEventListener listener : eventListeners) {
            listener.onAnimationEvent(this, eventType, animationName);
        }
    }

    /**
     * 获取骨骼变换副本（用于渲染）
     */
    public Map<String, float[]> getBoneTransforms() {
        return new HashMap<>(boneTransforms);
    }

    /**
     * 获取活跃的动画状态
     */
    public Map<String, AnimationState> getActiveAnimations() {
        Map<String, AnimationState> active = new HashMap<>();
        for (Map.Entry<String, AnimationState> entry : animationStates.entrySet()) {
            if (entry.getValue().isPlaying()) {
                active.put(entry.getKey(), entry.getValue());
            }
        }
        return active;
    }

    /**
     * 重置实例到初始状态
     */
    public void reset() {
        boneTransforms.clear();
        animationStates.clear();
        instanceProperties.clear();
        initializeBoneTransforms();
        transformDirty = true;
    }

    /**
     * 销毁实例
     */
    public void dispose() {
        // 停止所有动画
        for (AnimationState state : animationStates.values()) {
            state.stop();
        }

        // 清空资源
        boneTransforms.clear();
        animationStates.clear();
        instanceProperties.clear();
        eventListeners.clear();

        // 通知定义减少实例计数
        if (definition != null) {
            definition.decrementInstanceCount();
        }
    }

    // Getters
    public String getInstanceId() { return instanceId; }
    public ModelDefinition getDefinition() { return definition; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public boolean isTransformDirty() { return transformDirty; }
    public int getAnimationCount() { return animationStates.size(); }

    @Override
    public String toString() {
        return String.format("ModelInstance [%s, Definition: %s, ActiveAnimations: %d]",
                instanceId, definition.getName(), getActiveAnimations().size());
    }

    /**
     * 实例事件监听器接口
     */
    public interface InstanceEventListener {
        void onAnimationEvent(ModelInstance instance, String eventType, String animationName);
    }

    /**
     * 动画异常
     */
    public static class AnimationException extends Exception {
        public AnimationException(String message) {
            super(message);
        }

        public AnimationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
