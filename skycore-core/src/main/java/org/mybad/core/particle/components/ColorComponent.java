package org.mybad.core.particle.components;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.Component;
import org.mybad.core.particle.curve.Curve;

/**
 * 颜色组件 - 控制粒子颜色随生命周期变化
 * 支持RGBA颜色过度
 */
public class ColorComponent extends Component {

    private Curve redCurve;
    private Curve greenCurve;
    private Curve blueCurve;
    private Curve alphaCurve;

    private float initialR = 1.0f;
    private float initialG = 1.0f;
    private float initialB = 1.0f;
    private float initialA = 1.0f;

    public ColorComponent(String componentId, String componentName) {
        super(componentId, componentName);

        // 默认曲线
        this.redCurve = new Curve("red", Curve.CurveType.LINEAR);
        this.greenCurve = new Curve("green", Curve.CurveType.LINEAR);
        this.blueCurve = new Curve("blue", Curve.CurveType.LINEAR);
        this.alphaCurve = new Curve("alpha", Curve.CurveType.LINEAR);
    }

    @Override
    public void initialize() {
        // 初始化
    }

    @Override
    public void update(float deltaTime) {
        // 更新在apply中处理
    }

    @Override
    public void apply(Particle particle) {
        if (!enabled || particle == null) {
            return;
        }

        float progress = particle.getProgress();

        float r = initialR * redCurve.evaluate(progress);
        float g = initialG * greenCurve.evaluate(progress);
        float b = initialB * blueCurve.evaluate(progress);
        float a = initialA * alphaCurve.evaluate(progress);

        particle.setColor(r, g, b, a);
    }

    /**
     * 设置颜色曲线
     */
    public void setColorCurves(Curve red, Curve green, Curve blue, Curve alpha) {
        if (red != null) this.redCurve = red;
        if (green != null) this.greenCurve = green;
        if (blue != null) this.blueCurve = blue;
        if (alpha != null) this.alphaCurve = alpha;
    }

    /**
     * 设置初始颜色
     */
    public void setInitialColor(float r, float g, float b, float a) {
        this.initialR = r;
        this.initialG = g;
        this.initialB = b;
        this.initialA = a;
    }

    // Getters
    public Curve getRedCurve() { return redCurve; }
    public Curve getGreenCurve() { return greenCurve; }
    public Curve getBlueCurve() { return blueCurve; }
    public Curve getAlphaCurve() { return alphaCurve; }

    @Override
    public String toString() {
        return String.format("ColorComponent [%s, InitialColor: (%.2f, %.2f, %.2f, %.2f)]",
                componentId, initialR, initialG, initialB, initialA);
    }
}
