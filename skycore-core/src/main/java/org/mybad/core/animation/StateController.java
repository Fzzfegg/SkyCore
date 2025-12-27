package org.mybad.core.animation;

import java.util.*;
import java.util.function.Consumer;

/**
 * 状态控制器 - 管理状态机中的单个状态
 * 定义状态的行为、入口/退出回调、动画等
 */
public class StateController {

    private String stateName;
    private long entryTime;
    private long exitTime;
    private float stateTimer;
    private boolean active;

    // 回调函数
    private Consumer<StateController> onEnterCallback;
    private Consumer<StateController> onExitCallback;
    private Consumer<StateController> onUpdateCallback;

    // 关联动画
    private String associatedAnimation;
    private AnimationState animationState;

    // 状态参数
    private Map<String, Object> stateParameters;

    // 超时处理
    private float timeout;              // 状态超时时长（-1 = 无超时）
    private boolean timeoutExceeded;

    public StateController(String stateName) {
        this.stateName = stateName;
        this.entryTime = 0;
        this.exitTime = 0;
        this.stateTimer = 0f;
        this.active = false;
        this.stateParameters = new HashMap<>();
        this.timeout = -1f;
        this.timeoutExceeded = false;
    }

    /**
     * 进入状态
     */
    public void enter() {
        this.entryTime = System.currentTimeMillis();
        this.exitTime = 0;
        this.stateTimer = 0f;
        this.active = true;
        this.timeoutExceeded = false;

        // 启动关联动画
        if (associatedAnimation != null && animationState != null) {
            animationState.restart();
        }

        // 调用进入回调
        if (onEnterCallback != null) {
            onEnterCallback.accept(this);
        }
    }

    /**
     * 退出状态
     */
    public void exit() {
        this.exitTime = System.currentTimeMillis();
        this.active = false;

        // 停止关联动画
        if (animationState != null) {
            animationState.stop();
        }

        // 调用退出回调
        if (onExitCallback != null) {
            onExitCallback.accept(this);
        }
    }

    /**
     * 更新状态
     */
    public void update(float deltaTime) {
        if (!active) {
            return;
        }

        stateTimer += deltaTime;

        // 检查超时
        if (timeout > 0 && stateTimer >= timeout && !timeoutExceeded) {
            timeoutExceeded = true;
        }

        // 更新关联动画
        if (animationState != null) {
            animationState.update(deltaTime);
        }

        // 调用更新回调
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(this);
        }
    }

    /**
     * 关联动画
     */
    public void associateAnimation(String animationName, Animation animation) {
        this.associatedAnimation = animationName;
        this.animationState = new AnimationState(animation);
    }

    /**
     * 设置状态参数
     */
    public void setParameter(String key, Object value) {
        stateParameters.put(key, value);
    }

    /**
     * 获取状态参数
     */
    public Object getParameter(String key) {
        return stateParameters.get(key);
    }

    /**
     * 获取整数参数
     */
    public int getIntParameter(String key, int defaultValue) {
        Object value = stateParameters.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    /**
     * 获取浮点参数
     */
    public float getFloatParameter(String key, float defaultValue) {
        Object value = stateParameters.get(key);
        return value instanceof Float ? (Float) value : defaultValue;
    }

    /**
     * 获取布尔参数
     */
    public boolean getBoolParameter(String key, boolean defaultValue) {
        Object value = stateParameters.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    /**
     * 检查参数是否存在
     */
    public boolean hasParameter(String key) {
        return stateParameters.containsKey(key);
    }

    /**
     * 设置进入回调
     */
    public void setOnEnter(Consumer<StateController> callback) {
        this.onEnterCallback = callback;
    }

    /**
     * 设置退出回调
     */
    public void setOnExit(Consumer<StateController> callback) {
        this.onExitCallback = callback;
    }

    /**
     * 设置更新回调
     */
    public void setOnUpdate(Consumer<StateController> callback) {
        this.onUpdateCallback = callback;
    }

    /**
     * 设置状态超时
     */
    public void setTimeout(float timeout) {
        this.timeout = timeout;
        this.timeoutExceeded = false;
    }

    /**
     * 检查是否超时
     */
    public boolean isTimeoutExceeded() {
        return timeoutExceeded;
    }

    /**
     * 检查超时时间
     */
    public float getTimeoutRemaining() {
        if (timeout <= 0) {
            return -1;
        }
        return Math.max(0, timeout - stateTimer);
    }

    /**
     * 重置状态
     */
    public void reset() {
        stateTimer = 0f;
        timeoutExceeded = false;
        stateParameters.clear();
        if (animationState != null) {
            animationState.stop();
        }
    }

    // Getters
    public String getStateName() { return stateName; }
    public long getEntryTime() { return entryTime; }
    public long getExitTime() { return exitTime; }
    public float getStateTimer() { return stateTimer; }
    public boolean isActive() { return active; }
    public String getAssociatedAnimation() { return associatedAnimation; }
    public AnimationState getAnimationState() { return animationState; }
    public long getStateDuration() { return exitTime > entryTime ? exitTime - entryTime : System.currentTimeMillis() - entryTime; }

    @Override
    public String toString() {
        return String.format("StateController [%s, Active: %b, Timer: %.2fs, Animation: %s]",
                stateName, active, stateTimer, associatedAnimation != null ? associatedAnimation : "none");
    }
}
