package org.mybad.core.event;

import org.mybad.core.constraint.Constraint;
import org.mybad.core.data.*;

/**
 * 模型相关事件
 */
public class ModelEvents {
    public static final String MODEL_LOADED = "model.loaded";
    public static final String MODEL_UNLOADED = "model.unloaded";
    public static final String BONE_TRANSFORM_CHANGED = "bone.transform.changed";
    public static final String CONSTRAINT_APPLIED = "constraint.applied";

    /**
     * 模型加载事件
     */
    public static class ModelLoadedEvent extends Event {
        private Model model;

        public ModelLoadedEvent(Model model) {
            super(MODEL_LOADED);
            this.model = model;
        }

        public Model getModel() {
            return model;
        }
    }

    /**
     * 模型卸载事件
     */
    public static class ModelUnloadedEvent extends Event {
        private String modelName;

        public ModelUnloadedEvent(String modelName) {
            super(MODEL_UNLOADED);
            this.modelName = modelName;
        }

        public String getModelName() {
            return modelName;
        }
    }

    /**
     * 骨骼变换改变事件
     */
    public static class BoneTransformChangedEvent extends Event {
        private ModelBone bone;
        private TransformType type;

        public enum TransformType {
            POSITION, ROTATION, SCALE, ALL
        }

        public BoneTransformChangedEvent(ModelBone bone, TransformType type) {
            super(BONE_TRANSFORM_CHANGED);
            this.bone = bone;
            this.type = type;
        }

        public ModelBone getBone() {
            return bone;
        }

        public TransformType getTransformType() {
            return type;
        }

        @Override
        public boolean isCancellable() {
            return true;
        }
    }

    /**
     * 约束应用事件
     */
    public static class ConstraintAppliedEvent extends Event {
        private Constraint constraint;
        private ModelBone targetBone;
        private ModelBone sourceBone;

        public ConstraintAppliedEvent(Constraint constraint, ModelBone targetBone, ModelBone sourceBone) {
            super(CONSTRAINT_APPLIED);
            this.constraint = constraint;
            this.targetBone = targetBone;
            this.sourceBone = sourceBone;
        }

        public Constraint getConstraint() {
            return constraint;
        }

        public ModelBone getTargetBone() {
            return targetBone;
        }

        public ModelBone getSourceBone() {
            return sourceBone;
        }
    }

    /**
     * 骨骼渲染事件，可取消以跳过该骨骼渲染。
     */
    public static class RenderBoneEvent extends Event {
        private final ModelBone bone;
        private final float[] matrix;

        public RenderBoneEvent(ModelBone bone, float[] matrix) {
            super("model.render_bone");
            this.bone = bone;
            this.matrix = matrix;
        }

        public ModelBone getBone() {
            return bone;
        }

        public float[] getMatrix() {
            return matrix;
        }

        @Override
        public boolean isCancellable() {
            return true;
        }
    }

    /**
     * 立方体渲染事件，可取消以跳过该立方体。
     */
    public static class RenderCubeEvent extends Event {
        private final ModelBone bone;
        private final ModelCube cube;
        private final float[] matrix;

        public RenderCubeEvent(ModelBone bone, ModelCube cube, float[] matrix) {
            super("model.render_cube");
            this.bone = bone;
            this.cube = cube;
            this.matrix = matrix;
        }

        public ModelBone getBone() {
            return bone;
        }

        public ModelCube getCube() {
            return cube;
        }

        public float[] getMatrix() {
            return matrix;
        }

        @Override
        public boolean isCancellable() {
            return true;
        }
    }
}
