package org.mybad.core.legacy.expression;

import org.mybad.core.legacy.expression.molang.Molang;
import org.mybad.core.legacy.expression.molang.exception.MolangException;
import java.util.HashMap;
import java.util.Map;

/**
 * MoLang 表达式引擎包装器
 * 为整个 SkyCore 框架提供统一的表达式评估接口
 *
 * 支持：
 * - 基本数学运算（+、-、*、/）
 * - 比较操作（<、>、==、!=、<=、>=）
 * - 逻辑操作（&&、||、!）
 * - 三元操作（? :）
 * - 数学函数（sqrt, abs, floor, ceil 等）
 * - 变量替换（通过表达式上下文）
 *
 * 用途：
 * - 模型动画中的表达式计算（约束、动画参数）
 * - 粒子系统中的动态参数计算
 * - 任何需要动态数值计算的场景
 */
public class MolangExpressionEngine {

    private static final MolangExpressionEngine INSTANCE = new MolangExpressionEngine();

    private final Map<String, Float> globalVariables;
    private final Map<String, ExpressionCache> expressionCache;
    private final long CACHE_TIMEOUT = 60000; // 60 秒缓存超时

    /**
     * 表达式缓存项
     */
    private static class ExpressionCache {
        String expression;
        Float cachedValue;
        long lastEvaluateTime;

        ExpressionCache(String expression) {
            this.expression = expression;
            this.lastEvaluateTime = 0;
        }
    }

    private MolangExpressionEngine() {
        this.globalVariables = new HashMap<>();
        this.expressionCache = new HashMap<>();
    }

    /**
     * 获取单例实例
     */
    public static MolangExpressionEngine getInstance() {
        return INSTANCE;
    }

    /**
     * 评估 MoLang 表达式
     * @param expression MoLang 表达式字符串
     * @return 评估结果（float 类型）
     * @throws MolangException 如果表达式格式不正确
     */
    public float evaluate(String expression) throws MolangException {
        if (expression == null || expression.isEmpty()) {
            throw new MolangException("Expression cannot be null or empty");
        }

        try {
            return Molang.eval(expression);
        } catch (Exception e) {
            throw new MolangException("Failed to evaluate expression: " + expression, e);
        }
    }

    /**
     * 评估表达式，支持变量替换
     * @param expression MoLang 表达式字符串
     * @param variables 变量映射表
     * @return 评估结果
     * @throws MolangException 如果表达式格式不正确
     */
    public float evaluate(String expression, Map<String, Float> variables) throws MolangException {
        if (variables != null && !variables.isEmpty()) {
            // 替换表达式中的变量
            String processedExpression = replaceVariables(expression, variables);
            return evaluate(processedExpression);
        }
        return evaluate(expression);
    }

    /**
     * 设置全局变量
     * @param name 变量名
     * @param value 变量值
     */
    public void setVariable(String name, float value) {
        globalVariables.put(name, value);
    }

    /**
     * 获取全局变量
     * @param name 变量名
     * @return 变量值，如果不存在返回 0.0f
     */
    public float getVariable(String name) {
        return globalVariables.getOrDefault(name, 0.0f);
    }

    /**
     * 清空所有全局变量
     */
    public void clearVariables() {
        globalVariables.clear();
    }

    /**
     * 检查变量是否存在
     * @param name 变量名
     * @return true 如果变量存在
     */
    public boolean hasVariable(String name) {
        return globalVariables.containsKey(name);
    }

    /**
     * 替换表达式中的变量占位符
     * @param expression 原始表达式
     * @param variables 变量映射表
     * @return 替换后的表达式
     */
    private String replaceVariables(String expression, Map<String, Float> variables) {
        String result = expression;

        for (Map.Entry<String, Float> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }

        return result;
    }

    /**
     * 验证表达式语法
     * @param expression 要验证的表达式
     * @return true 如果表达式有效
     */
    public boolean isValidExpression(String expression) {
        try {
            evaluate(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取表达式的描述信息
     * @return 引擎信息
     */
    public String getEngineInfo() {
        return "MoLang Expression Engine (Arcane)" +
                "\nSupported Operations: +, -, *, /, <, >, ==, !=, <=, >=, &&, ||, !" +
                "\nSupported Functions: math.sqrt, math.abs, math.floor, math.ceil, etc." +
                "\nGlobal Variables: " + globalVariables.size();
    }

    /**
     * 清空表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
    }
}
