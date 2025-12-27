package org.mybad.core.animationsystem;

import java.util.function.Predicate;

/**
 * 状态转换
 * 定义从一个状态到另一个状态的转换规则
 * 支持条件判断、优先级、自动检查
 */
public class StateTransition {
    private String fromState;      // null 表示任意状态
    private String toState;
    private Predicate<TransitionContext> condition;
    private float transitionDuration;
    private int priority;
    private boolean automatic;

    // 私有构造函数，通过 Builder 创建
    private StateTransition(String fromState, String toState,
                           Predicate<TransitionContext> condition,
                           float transitionDuration, int priority, boolean automatic) {
        this.fromState = fromState;
        this.toState = toState;
        this.condition = condition;
        this.transitionDuration = transitionDuration;
        this.priority = priority;
        this.automatic = automatic;
    }

    /**
     * 检查是否可以执行此转换
     * @param ctx 转换上下文
     * @return 是否满足转换条件
     */
    public boolean canTransition(TransitionContext ctx) {
        // 检查源状态
        String currentStateName = ctx.getCurrentStateName();
        if (fromState != null && !fromState.equals(currentStateName)) {
            return false;
        }

        // 检查条件
        if (condition != null) {
            return condition.test(ctx);
        }

        return false;
    }

    // Getter 方法

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }

    public Predicate<TransitionContext> getCondition() {
        return condition;
    }

    public float getTransitionDuration() {
        return transitionDuration;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    @Override
    public String toString() {
        return String.format("StateTransition[%s -> %s, priority=%d, auto=%s]",
                fromState != null ? fromState : "ANY", toState, priority, automatic);
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private String fromState;
        private String toState;
        private Predicate<TransitionContext> condition;
        private float transitionDuration = 0.0f;
        private int priority = 0;
        private boolean automatic = true;

        public Builder from(String state) {
            this.fromState = state;
            return this;
        }

        public Builder to(String state) {
            this.toState = state;
            return this;
        }

        public Builder when(Predicate<TransitionContext> condition) {
            this.condition = condition;
            return this;
        }

        public Builder withDuration(float duration) {
            this.transitionDuration = Math.max(0, duration);
            return this;
        }

        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder automatic(boolean auto) {
            this.automatic = auto;
            return this;
        }

        public StateTransition build() {
            if (toState == null) {
                throw new IllegalArgumentException("转换必须指定目标状态 (toState)");
            }
            if (condition == null) {
                throw new IllegalArgumentException("转换必须指定条件 (when)");
            }
            return new StateTransition(fromState, toState, condition,
                    transitionDuration, priority, automatic);
        }
    }
}
