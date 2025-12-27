package org.mybad.core.particle.expression;

import org.mybad.core.particle.Particle;
import org.mybad.core.expression.MolangExpressionEngine;
import org.mybad.core.expression.molang.exception.MolangException;
import java.util.*;

/**
 * 粒子表达式修饰器 - 使用MoLang表达式动态修改粒子参数
 * 支持动态计算粒子属性
 */
public class ParticleExpressionModifier {

    private String modifierId;
    private MolangExpressionEngine engine;

    // 表达式和绑定
    private Map<String, String> expressions;  // 属性名 -> 表达式
    private Map<String, ParticleProperty> propertyBindings;

    // 缓存上下文
    private Map<String, Float> expressionContext;

    // 性能统计
    private int evaluationCount = 0;
    private long totalEvaluationTime = 0;

    public ParticleExpressionModifier(String modifierId) {
        this.modifierId = modifierId;
        this.engine = MolangExpressionEngine.getInstance();
        this.expressions = new HashMap<>();
        this.propertyBindings = new HashMap<>();
        this.expressionContext = new HashMap<>();

        // 注册标准属性
        registerProperty("position_x", (p, v) -> p.setPosition(v, p.getPositionY(), p.getPositionZ()));
        registerProperty("position_y", (p, v) -> p.setPosition(p.getPositionX(), v, p.getPositionZ()));
        registerProperty("position_z", (p, v) -> p.setPosition(p.getPositionX(), p.getPositionY(), v));

        registerProperty("velocity_x", (p, v) -> p.setVelocity(v, p.getVelocityY(), p.getVelocityZ()));
        registerProperty("velocity_y", (p, v) -> p.setVelocity(p.getVelocityX(), v, p.getVelocityZ()));
        registerProperty("velocity_z", (p, v) -> p.setVelocity(p.getVelocityX(), p.getVelocityY(), v));

        registerProperty("scale", (p, v) -> p.setScale(v, v, v));
        registerProperty("color_r", (p, v) -> p.setColor(v, p.getColorG(), p.getColorB(), p.getColorA()));
        registerProperty("color_a", (p, v) -> p.setColor(p.getColorR(), p.getColorG(), p.getColorB(), v));
    }

    /**
     * 注册表达式
     */
    public void registerExpression(String propertyName, String expression) {
        expressions.put(propertyName, expression);
    }

    /**
     * 注册属性绑定
     */
    public void registerProperty(String propertyName, ParticleProperty property) {
        propertyBindings.put(propertyName, property);
    }

    /**
     * 构建表达式上下文
     */
    private void buildContext(Particle particle) {
        expressionContext.clear();

        // 粒子信息
        expressionContext.put("age", particle.getAge());
        expressionContext.put("life", particle.getMaxAge());
        expressionContext.put("progress", particle.getProgress());

        // 位置
        expressionContext.put("position_x", particle.getPositionX());
        expressionContext.put("position_y", particle.getPositionY());
        expressionContext.put("position_z", particle.getPositionZ());

        // 速度
        expressionContext.put("velocity_x", particle.getVelocityX());
        expressionContext.put("velocity_y", particle.getVelocityY());
        expressionContext.put("velocity_z", particle.getVelocityZ());

        // 缩放和颜色
        expressionContext.put("scale", particle.getScaleX());
        expressionContext.put("color_r", particle.getColorR());
        expressionContext.put("color_a", particle.getColorA());
    }

    /**
     * 应用表达式到粒子
     */
    public void apply(Particle particle) {
        if (expressions.isEmpty()) {
            return;
        }

        buildContext(particle);
        long startTime = System.nanoTime();

        for (Map.Entry<String, String> entry : expressions.entrySet()) {
            String propertyName = entry.getKey();
            String expression = entry.getValue();

            try {
                // 评估表达式
                float value = engine.evaluate(expression, expressionContext);

                // 应用到粒子
                ParticleProperty property = propertyBindings.get(propertyName);
                if (property != null) {
                    property.apply(particle, value);
                }

                evaluationCount++;

            } catch (MolangException e) {
                // 表达式错误，记录但继续
                System.err.println("Expression error: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        totalEvaluationTime += (endTime - startTime);
    }

    /**
     * 清空所有表达式
     */
    public void clear() {
        expressions.clear();
    }

    /**
     * 获取性能统计
     */
    public String getStats() {
        double avgTime = evaluationCount > 0 ? totalEvaluationTime / (double) evaluationCount / 1000000.0 : 0;
        return String.format("ExpressionModifier [%s, Evals: %d, AvgTime: %.3fms]",
                modifierId, evaluationCount, avgTime);
    }

    // Getters
    public String getModifierId() { return modifierId; }
    public int getEvaluationCount() { return evaluationCount; }

    /**
     * 粒子属性接口
     */
    @FunctionalInterface
    public interface ParticleProperty {
        void apply(Particle particle, float value);
    }

    @Override
    public String toString() {
        return getStats();
    }
}
