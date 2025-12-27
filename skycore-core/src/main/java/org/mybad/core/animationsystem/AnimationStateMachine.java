package org.mybad.core.animationsystem;

import java.util.*;

/**
 * 动画状态机
 * 管理多个动画状态和它们之间的转换
 * 支持自动和手动状态转换
 */
public class AnimationStateMachine {
    private String machineId;
    private String currentStateName;
    private StateController currentController;
    private Map<String, StateController> states;
    private List<StateTransition> transitions;
    private TransitionContext context;
    private boolean autoTransition;

    /**
     * 创建状态机
     * @param machineId 状态机ID
     */
    public AnimationStateMachine(String machineId) {
        this.machineId = machineId;
        this.states = new HashMap<>();
        this.transitions = new ArrayList<>();
        this.context = new TransitionContext();
        this.autoTransition = true;
    }

    // 状态管理方法

    /**
     * 添加状态
     */
    public void addState(String name, StateController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("StateController 不能为 null");
        }
        states.put(name, controller);
    }

    /**
     * 移除状态
     */
    public void removeState(String name) {
        states.remove(name);
        if (currentStateName != null && currentStateName.equals(name)) {
            currentStateName = null;
            currentController = null;
        }
    }

    /**
     * 获取状态
     */
    public StateController getState(String name) {
        return states.get(name);
    }

    /**
     * 获取所有状态
     */
    public Collection<StateController> getAllStates() {
        return states.values();
    }

    // 转换管理方法

    /**
     * 添加状态转换
     */
    public void addTransition(StateTransition transition) {
        if (transition == null) {
            throw new IllegalArgumentException("StateTransition 不能为 null");
        }
        transitions.add(transition);
        // 按优先级排序（高优先级排前面）
        transitions.sort((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
    }

    /**
     * 添加状态转换（简化方法）
     */
    public void addTransition(String from, String to, java.util.function.Predicate<TransitionContext> condition) {
        StateTransition transition = new StateTransition.Builder()
                .from(from)
                .to(to)
                .when(condition)
                .automatic(true)
                .build();
        addTransition(transition);
    }

    /**
     * 移除转换
     */
    public void removeTransition(StateTransition transition) {
        transitions.remove(transition);
    }

    /**
     * 获取所有转换
     */
    public List<StateTransition> getAllTransitions() {
        return new ArrayList<>(transitions);
    }

    // 状态切换方法

    /**
     * 转换到指定状态
     */
    public void transitionTo(String stateName) {
        transitionTo(stateName, 0.0f);
    }

    /**
     * 转换到指定状态（带过渡时间）
     */
    public void transitionTo(String stateName, float duration) {
        if (!states.containsKey(stateName)) {
            throw new IllegalArgumentException("状态 '" + stateName + "' 不存在");
        }
        performTransition(stateName, duration);
    }

    /**
     * 执行转换
     */
    private void performTransition(String toState, float duration) {
        // 退出旧状态
        if (currentController != null) {
            currentController.exit();
        }

        // 进入新状态
        currentStateName = toState;
        currentController = states.get(toState);

        if (currentController != null) {
            currentController.enter();
        }
    }

    // 更新循环

    /**
     * 更新状态机
     */
    public void update(float deltaTime) {
        // 更新上下文
        context.setDeltaTime(deltaTime);
        context.setCurrentState(currentController);

        // 更新当前状态
        if (currentController != null) {
            currentController.update(deltaTime);
        }

        // 检查自动转换
        if (autoTransition) {
            checkTransitions();
        }
    }

    /**
     * 检查转换条件
     */
    private void checkTransitions() {
        for (StateTransition transition : transitions) {
            // 只检查自动转换
            if (!transition.isAutomatic()) {
                continue;
            }

            // 检查源状态
            if (transition.getFromState() != null &&
                    !transition.getFromState().equals(currentStateName)) {
                continue;
            }

            // 检查条件
            if (transition.canTransition(context)) {
                performTransition(transition.getToState(), transition.getTransitionDuration());
                return;  // 每次更新只进行一次转换
            }
        }
    }

    // Getter 方法

    public String getMachineId() {
        return machineId;
    }

    public String getCurrentStateName() {
        return currentStateName;
    }

    public StateController getCurrentController() {
        return currentController;
    }

    public TransitionContext getContext() {
        return context;
    }

    public boolean isAutoTransitionEnabled() {
        return autoTransition;
    }

    /**
     * 设置是否启用自动转换
     */
    public void setAutoTransition(boolean auto) {
        this.autoTransition = auto;
    }

    /**
     * 获取状态数量
     */
    public int getStateCount() {
        return states.size();
    }

    /**
     * 获取转换数量
     */
    public int getTransitionCount() {
        return transitions.size();
    }

    @Override
    public String toString() {
        return String.format("AnimationStateMachine[%s, current=%s, states=%d, transitions=%d]",
                machineId, currentStateName, states.size(), transitions.size());
    }
}
