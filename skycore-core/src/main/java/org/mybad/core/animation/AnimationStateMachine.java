package org.mybad.core.animation;

import java.util.*;

/**
 * 动画状态机 - 管理动画状态和转换
 * 支持自动转换、优先级管理、超时处理
 * 用于复杂的动画切换逻辑（如运动状态、交互等）
 */
public class AnimationStateMachine {

    private String machineId;
    private String currentState;
    private StateController currentStateController;

    // 所有状态和转换
    private Map<String, StateController> states;
    private List<StateTransition> transitions;

    // 过渡动画
    private String defaultTransitionAnimation;
    private float defaultTransitionDuration;

    // 事件监听
    private List<StateMachineListener> listeners;

    // 性能追踪
    private int transitionCount;
    private long lastTransitionTime;

    public AnimationStateMachine(String machineId) {
        this.machineId = machineId;
        this.states = new HashMap<>();
        this.transitions = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.transitionCount = 0;
        this.lastTransitionTime = 0;
        this.defaultTransitionDuration = 0.5f;
    }

    /**
     * 添加状态
     */
    public void addState(String stateName) {
        StateController controller = new StateController(stateName);
        states.put(stateName, controller);
    }

    /**
     * 添加状态并关联动画
     */
    public void addState(String stateName, String animationName, Animation animation) {
        StateController controller = new StateController(stateName);
        controller.associateAnimation(animationName, animation);
        states.put(stateName, controller);
    }

    /**
     * 获取状态控制器
     */
    public StateController getState(String stateName) {
        return states.get(stateName);
    }

    /**
     * 获取当前状态控制器
     */
    public StateController getCurrentStateController() {
        return currentStateController;
    }

    /**
     * 添加转换
     */
    public void addTransition(StateTransition transition) {
        transitions.add(transition);
        // 按优先级排序
        transitions.sort((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
    }

    /**
     * 添加转换（便捷方法）
     */
    public void addTransition(String fromState, String toState, java.util.function.Predicate<StateTransition.TransitionContext> condition) {
        StateTransition transition = new StateTransition(fromState, toState, condition, defaultTransitionDuration);
        addTransition(transition);
    }

    /**
     * 设置初始状态
     */
    public void setInitialState(String stateName) throws StateMachineException {
        StateController controller = states.get(stateName);
        if (controller == null) {
            throw new StateMachineException("State not found: " + stateName);
        }

        if (currentStateController != null) {
            currentStateController.exit();
        }

        currentState = stateName;
        currentStateController = controller;
        currentStateController.enter();
        fireStateChangedEvent(null, stateName);
    }

    /**
     * 转换到指定状态
     */
    public boolean transitionTo(String targetState) {
        return transitionTo(targetState, null);
    }

    /**
     * 转换到指定状态（强制转换）
     */
    public boolean transitionTo(String targetState, String transitionName) {
        StateController targetController = states.get(targetState);
        if (targetController == null) {
            return false;
        }

        if (currentState.equals(targetState)) {
            return false; // 已在目标状态
        }

        // 退出当前状态
        if (currentStateController != null) {
            currentStateController.exit();
        }

        // 进入新状态
        String previousState = currentState;
        currentState = targetState;
        currentStateController = targetController;
        currentStateController.enter();

        transitionCount++;
        lastTransitionTime = System.currentTimeMillis();

        fireStateChangedEvent(previousState, targetState);
        return true;
    }

    /**
     * 更新状态机
     * 检查转换条件并自动转换
     */
    public void update(float deltaTime) {
        if (currentStateController == null) {
            return;
        }

        // 更新当前状态
        currentStateController.update(deltaTime);

        // 检查自动转换
        checkTransitions(deltaTime);
    }

    /**
     * 检查是否可以转换
     */
    private void checkTransitions(float deltaTime) {
        StateTransition.TransitionContext context = new StateTransition.TransitionContext(
                currentState,
                System.currentTimeMillis(),
                deltaTime
        );

        // 按优先级检查转换
        for (StateTransition transition : transitions) {
            // 检查转换是否来自当前状态
            if (!transition.getSourceState().equals(currentState)) {
                continue;
            }

            // 检查是否是自动转换
            if (!transition.isAutomatic()) {
                continue;
            }

            // 检查转换条件
            if (transition.canTransition(context)) {
                transitionTo(transition.getTargetState());
                return; // 一次只转换一个
            }
        }
    }

    /**
     * 检查是否可以转换到指定状态
     */
    public boolean canTransitionTo(String targetState) {
        for (StateTransition transition : transitions) {
            if (transition.getSourceState().equals(currentState) &&
                    transition.getTargetState().equals(targetState)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有可能的目标状态
     */
    public List<String> getPossibleNextStates() {
        List<String> possibleStates = new ArrayList<>();
        for (StateTransition transition : transitions) {
            if (transition.getSourceState().equals(currentState)) {
                possibleStates.add(transition.getTargetState());
            }
        }
        return possibleStates;
    }

    /**
     * 添加状态机监听器
     */
    public void addListener(StateMachineListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除状态机监听器
     */
    public void removeListener(StateMachineListener listener) {
        listeners.remove(listener);
    }

    /**
     * 触发状态改变事件
     */
    private void fireStateChangedEvent(String previousState, String currentState) {
        for (StateMachineListener listener : listeners) {
            listener.onStateChanged(this, previousState, currentState);
        }
    }

    /**
     * 重置状态机
     */
    public void reset() {
        for (StateController state : states.values()) {
            state.reset();
        }
        currentState = null;
        currentStateController = null;
        transitionCount = 0;
        lastTransitionTime = 0;
    }

    /**
     * 获取状态机信息
     */
    public String getStateMachineInfo() {
        return String.format("StateMachine [%s, Current: %s, States: %d, Transitions: %d, Changes: %d]",
                machineId, currentState, states.size(), transitions.size(), transitionCount);
    }

    // Getters
    public String getMachineId() { return machineId; }
    public String getCurrentState() { return currentState; }
    public int getStateCount() { return states.size(); }
    public int getTransitionCount() { return transitionCount; }
    public long getLastTransitionTime() { return lastTransitionTime; }
    public Map<String, StateController> getAllStates() { return new HashMap<>(states); }

    @Override
    public String toString() {
        return getStateMachineInfo();
    }

    /**
     * 状态机监听器接口
     */
    public interface StateMachineListener {
        void onStateChanged(AnimationStateMachine machine, String previousState, String currentState);
    }

    /**
     * 状态机异常
     */
    public static class StateMachineException extends Exception {
        public StateMachineException(String message) {
            super(message);
        }

        public StateMachineException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
