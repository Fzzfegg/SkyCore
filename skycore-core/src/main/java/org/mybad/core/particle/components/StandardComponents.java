package org.mybad.core.particle.components;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.Component;
import java.util.*;

/**
 * 标准组件集合
 * 定义所有标准的粒子组件
 */

/**
 * 生命周期组件
 */
class LifetimeComponent extends Component {
    public LifetimeComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}
}

/**
 * 初始速度组件
 */
class InitialSpeedComponent extends Component {
    private float minSpeed = 0;
    private float maxSpeed = 1;

    public InitialSpeedComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setSpeedRange(float min, float max) { this.minSpeed = min; this.maxSpeed = max; }
}

/**
 * 初始旋转组件
 */
class InitialSpinComponent extends Component {
    private float spinSpeed = 0;

    public InitialSpinComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {
        if (enabled && particle != null) {
            particle.setRotationSpeed(spinSpeed, spinSpeed, spinSpeed);
        }
    }

    public void setSpinSpeed(float speed) { this.spinSpeed = speed; }
}

/**
 * 碰撞组件
 */
class CollisionComponent extends Component {
    private boolean bouncy = false;
    private float bounciness = 0.5f;

    public CollisionComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setBouncy(boolean bouncy) { this.bouncy = bouncy; }
    public void setBounciness(float bounce) { this.bounciness = bounce; }
}

/**
 * 子发射器组件
 */
class SubEmitterComponent extends Component {
    private String emitterReference;
    private int emitCount = 1;

    public SubEmitterComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setEmitterReference(String ref) { this.emitterReference = ref; }
    public void setEmitCount(int count) { this.emitCount = count; }
}

/**
 * 物理组件
 */
class PhysicsComponent extends Component {
    private float mass = 1.0f;
    private float drag = 0.99f;

    public PhysicsComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setMass(float mass) { this.mass = mass; }
    public void setDrag(float drag) { this.drag = drag; }
}

/**
 * 吸引力组件
 */
class AttractorComponent extends Component {
    private float attractorX = 0;
    private float attractorY = 0;
    private float attractorZ = 0;
    private float force = 1.0f;
    private float range = Float.MAX_VALUE;

    public AttractorComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setAttractorPosition(float x, float y, float z) {
        this.attractorX = x;
        this.attractorY = y;
        this.attractorZ = z;
    }

    public void setForce(float force) { this.force = force; }
    public void setRange(float range) { this.range = range; }
}

/**
 * Billboard组件
 */
class BillboardComponent extends Component {
    private boolean faceCamera = true;
    private boolean faceX = true;
    private boolean faceY = true;
    private boolean faceZ = true;

    public BillboardComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setFaceCamera(boolean face) { this.faceCamera = face; }
    public void setFaceAxis(boolean x, boolean y, boolean z) {
        this.faceX = x;
        this.faceY = y;
        this.faceZ = z;
    }
}

/**
 * 光照组件
 */
class LightingComponent extends Component {
    private float emissionStrength = 1.0f;
    private boolean castShadow = false;

    public LightingComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setEmissionStrength(float strength) { this.emissionStrength = strength; }
    public void setCastShadow(boolean cast) { this.castShadow = cast; }
}

/**
 * 纹理组件
 */
class TextureComponent extends Component {
    private String textureFile;
    private int spriteColumns = 1;
    private int spriteRows = 1;
    private float animationSpeed = 1.0f;

    public TextureComponent(String id, String name) { super(id, name); }
    @Override public void initialize() {}
    @Override public void update(float deltaTime) {}
    @Override public void apply(Particle particle) {}

    public void setTexture(String file) { this.textureFile = file; }
    public void setSpriteAnimation(int cols, int rows, float speed) {
        this.spriteColumns = cols;
        this.spriteRows = rows;
        this.animationSpeed = speed;
    }
}
