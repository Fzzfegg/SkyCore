package org.mybad.core.animation;

import java.util.function.Predicate;

/**
 * 状态转换 - 定义状态间的转换关系
 * 包含源状态、目标状态和转换条件
 */
public class StateTransition {

    private String fromState;
    private String toState;
    private Predicate<TransitionContext> condition;
    private float transitionDuration;  // 转换动画时长
    private int priority;               // 优先级（高优先级先检查）
    private boolean automatic;          // 是否自动检查转换
    private String description;         // 描述

    public StateTransition(String fromState, String toState) {
        this(fromState, toState, ctx -> true, 0.5f, 0);
    }

    public StateTransition(String fromState, String toState, Predicate<TransitionContext> condition) {
        this(fromState, toState, condition, 0.5f, 0);
    }

    public StateTransition(String fromState, String toState, Predicate<TransitionContext> condition, float duration) {
        this(fromState, toState, condition, duration, 0);
    }

    public StateTransition(String fromState, String toState, Predicate<TransitionContext> condition, float duration, int priority) {
        this.fromState = fromState;
        this.toState = toState;
        this.condition = condition;
        this.transitionDuration = duration;
        this.priority = priority;
        this.automatic = true;
        this.description = fromState + " -> " + toState;
    }

    /**
     * 检查转换条件是否满足
     */
    public boolean canTransition(TransitionContext context) {
        if (condition == null) {
            return true;
        }
        return condition.test(context);
    }

    /**
     * 获取转换目标状态
     */
    public String getTargetState() {
        return toState;
    }

    /**
     * 获取转换源状态
     */
    public String getSourceState() {
        return fromState;
    }

    /**
     * 获取转换时长
     */
    public float getTransitionDuration() {
        return transitionDuration;
    }

    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 是否自动转换
     */
    public boolean isAutomatic() {
        return automatic;
    }

    /**
     * 设置自动转换
     */
    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    /**
     * 设置描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("StateTransition [%s -> %s, Duration: %.2fs, Priority: %d, Auto: %b]",
                fromState, toState, transitionDuration, priority, automatic);
    }

    /**
     * 转换上下文 - 用于评估转换条件
     */
    public static class TransitionContext {
        private String currentState;
        private long currentTime;
        private float deltaTime;
        private Object customData;

        public TransitionContext(String currentState, long currentTime, float deltaTime) {
            this.currentState = currentState;
            this.currentTime = currentTime;
            this.deltaTime = deltaTime;
        }

        public String getCurrentState() { return currentState; }
        public long getCurrentTime() { return currentTime; }
        public float getDeltaTime() { return deltaTime; }
        public Object getCustomData() { return customData; }
        public void setCustomData(Object data) { this.customData = data; }
    }
}
