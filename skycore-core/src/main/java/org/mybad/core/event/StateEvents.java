package org.mybad.core.event;

import org.mybad.core.animationsystem.AnimationStateMachine;
import org.mybad.core.animationsystem.StateController;

/**
 * 动画状态机事件集合
 * 定义状态转换过程中触发的各种事件
 */
public class StateEvents {

    /**
     * 状态进入事件
     * 在新状态被激活时触发
     */
    public static class StateEnterEvent extends Event {
        private final AnimationStateMachine machine;
        private final String stateName;
        private final StateController controller;

        public StateEnterEvent(AnimationStateMachine machine, String stateName, StateController controller) {
            super("state_enter");
            this.machine = machine;
            this.stateName = stateName;
            this.controller = controller;
        }

        public AnimationStateMachine getMachine() {
            return machine;
        }

        public String getStateName() {
            return stateName;
        }

        public StateController getController() {
            return controller;
        }

        @Override
        public String toString() {
            return String.format("StateEnterEvent[machine=%s, state=%s]",
                    machine.getMachineId(), stateName);
        }
    }

    /**
     * 状态退出事件
     * 在状态即将被离开时触发
     */
    public static class StateExitEvent extends Event {
        private final AnimationStateMachine machine;
        private final String stateName;
        private final StateController controller;

        public StateExitEvent(AnimationStateMachine machine, String stateName, StateController controller) {
            super("state_exit");
            this.machine = machine;
            this.stateName = stateName;
            this.controller = controller;
        }

        public AnimationStateMachine getMachine() {
            return machine;
        }

        public String getStateName() {
            return stateName;
        }

        public StateController getController() {
            return controller;
        }

        @Override
        public String toString() {
            return String.format("StateExitEvent[machine=%s, state=%s]",
                    machine.getMachineId(), stateName);
        }
    }

    /**
     * 状态转换事件
     * 在状态即将转换时触发
     */
    public static class StateTransitionEvent extends Event {
        private final AnimationStateMachine machine;
        private final String fromState;
        private final String toState;
        private final float duration;

        public StateTransitionEvent(AnimationStateMachine machine, String fromState,
                                   String toState, float duration) {
            super("state_transition");
            this.machine = machine;
            this.fromState = fromState;
            this.toState = toState;
            this.duration = duration;
        }

        public AnimationStateMachine getMachine() {
            return machine;
        }

        public String getFromState() {
            return fromState;
        }

        public String getToState() {
            return toState;
        }

        public float getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return String.format("StateTransitionEvent[machine=%s, %s -> %s, duration=%.2f]",
                    machine.getMachineId(), fromState, toState, duration);
        }
    }

    /**
     * 状态超时事件
     * 在状态超时时触发
     */
    public static class StateTimeoutEvent extends Event {
        private final AnimationStateMachine machine;
        private final StateController controller;
        private final float timeout;

        public StateTimeoutEvent(AnimationStateMachine machine, StateController controller, float timeout) {
            super("state_timeout");
            this.machine = machine;
            this.controller = controller;
            this.timeout = timeout;
        }

        public AnimationStateMachine getMachine() {
            return machine;
        }

        public StateController getController() {
            return controller;
        }

        public float getTimeout() {
            return timeout;
        }

        @Override
        public String toString() {
            return String.format("StateTimeoutEvent[machine=%s, state=%s, timeout=%.2f]",
                    machine.getMachineId(), controller.getStateName(), timeout);
        }
    }
}
