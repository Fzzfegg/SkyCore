package org.mybad.core.particle.components;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.Component;
import org.mybad.core.particle.curve.Curve;

/**
 * 缩放组件 - 控制粒子大小随生命周期变化
 */
public class ScaleComponent extends Component {

    private Curve scaleCurve;
    private float initialScale = 1.0f;

    public ScaleComponent(String componentId, String componentName) {
        super(componentId, componentName);
        this.scaleCurve = new Curve("scale", Curve.CurveType.LINEAR);
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
        float scale = initialScale * scaleCurve.evaluate(progress);

        particle.setScale(scale, scale, scale);
    }

    /**
     * 设置缩放曲线
     */
    public void setScaleCurve(Curve curve) {
        if (curve != null) {
            this.scaleCurve = curve;
        }
    }

    /**
     * 设置初始缩放
     */
    public void setInitialScale(float scale) {
        this.initialScale = scale;
    }

    // Getters
    public Curve getScaleCurve() { return scaleCurve; }
    public float getInitialScale() { return initialScale; }

    @Override
    public String toString() {
        return String.format("ScaleComponent [%s, InitialScale: %.2f]",
                componentId, initialScale);
    }
}
