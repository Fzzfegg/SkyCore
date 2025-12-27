package org.mybad.core.animationsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换上下文
 * 为状态转换条件提供运行时变量和状态信息
 * 支持类型安全的变量访问
 */
public class TransitionContext {
    private Map<String, Object> variables;
    private StateController currentState;
    private float deltaTime;

    public TransitionContext() {
        this.variables = new HashMap<>();
    }

    // 变量访问方法

    /**
     * 设置上下文变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取上下文变量（通用）
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    /**
     * 获取布尔值变量
     */
    public boolean getBoolean(String key) {
        Object value = variables.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * 获取浮点数变量
     */
    public float getFloat(String key) {
        Object value = variables.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0.0f;
    }

    /**
     * 获取整数变量
     */
    public int getInt(String key) {
        Object value = variables.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 获取字符串变量
     */
    public String getString(String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 检查变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 清除所有变量
     */
    public void clearVariables() {
        variables.clear();
    }

    // 状态查询方法

    /**
     * 获取当前状态控制器
     */
    public StateController getCurrentState() {
        return currentState;
    }

    /**
     * 设置当前状态控制器
     */
    public void setCurrentState(StateController state) {
        this.currentState = state;
    }

    /**
     * 获取当前状态名称
     */
    public String getCurrentStateName() {
        return currentState != null ? currentState.getStateName() : null;
    }

    /**
     * 获取当前状态已持续时间（秒）
     */
    public float getStateTime() {
        return currentState != null ? currentState.getStateTimer() : 0.0f;
    }

    /**
     * 获取帧时间
     */
    public float getDeltaTime() {
        return deltaTime;
    }

    /**
     * 设置帧时间
     */
    public void setDeltaTime(float deltaTime) {
        this.deltaTime = deltaTime;
    }
}
