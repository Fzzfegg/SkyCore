package org.mybad.core.legacy.render;

import org.mybad.core.constraint.Constraint;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.render.CoreMatrixStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于访问者模式的模型渲染调度器：
 * <ul>
 *     <li>使用矩阵堆栈推导骨骼层级变换（Chameleon 的方式）；</li>
 *     <li>允许通过 {@link ModelProcessor} 在关键节点插入自定义逻辑（HammerAnimations 的可扩展事件理念）；</li>
 * </ul>
 */
public final class ModelRenderer {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;
    private static final float POSITION_EPSILON = 1e-4f;

    private ModelRenderer() {
    }

    public static void render(Model model, ModelProcessor processor) {
        render(model, processor, RenderOptions.builder().build());
    }

    public static void render(Model model, ModelProcessor processor, CoreMatrixStack stack) {
        render(model, processor, RenderOptions.builder().matrixStack(stack).build());
    }

    public static void render(Model model, ModelProcessor processor, RenderOptions options) {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(processor, "ModelProcessor cannot be null");

        RenderOptions opts = options != null ? options : RenderOptions.builder().build();
        CoreMatrixStack stack = opts.matrixStack != null ? opts.matrixStack : new CoreMatrixStack();

        if (opts.applyConstraints) {
            applyConstraints(model);
        }

        float textureWidth = opts.textureWidth > 0 ? opts.textureWidth : resolveTextureSize(model.getTextureWidth(), 64f);
        float textureHeight = opts.textureHeight > 0 ? opts.textureHeight : resolveTextureSize(model.getTextureHeight(), 64f);

        processor.onStart(model, stack);

        List<ModelBone> rootBones = collectRootBones(model.getBones());
        for (ModelBone bone : rootBones) {
            renderBone(model, bone, processor, stack, opts, textureWidth, textureHeight);
        }

        processor.onFinish(model, stack);
    }

    private static List<ModelBone> collectRootBones(List<ModelBone> bones) {
        List<ModelBone> roots = new ArrayList<>();
        for (ModelBone bone : bones) {
            if (bone.getParent() == null) {
                roots.add(bone);
            }
        }
        return roots;
    }

    private static void renderBone(Model model,
                                   ModelBone bone,
                                   ModelProcessor processor,
                                   CoreMatrixStack stack,
                                   RenderOptions options,
                                   float textureWidth,
                                   float textureHeight) {
        stack.push();
        applyBoneTransform(bone, stack);

        boolean traverseChildren = processor.beforeBone(model, bone, stack);

        if (!bone.isNeverRender()) {
            for (ModelCube cube : bone.getCubes()) {
                stack.push();
                applyCubeTransform(cube, stack);

                processor.renderCube(model, bone, cube, textureWidth, textureHeight, stack);

                stack.pop();
            }
        }

        if (traverseChildren) {
            for (ModelBone child : bone.getChildren()) {
                renderBone(model, child, processor, stack, options, textureWidth, textureHeight);
            }
        }

        processor.afterBone(model, bone, stack);
        stack.pop();
    }

    private static void applyBoneTransform(ModelBone bone, CoreMatrixStack stack) {
        float[] position = bone.getPosition();
        float translateX = convertX(position[0]);
        float translateY = convertY(position[1]);
        float translateZ = convertZ(position[2]);

        if (translateX != 0 || translateY != 0 || translateZ != 0) {
            stack.translate(translateX, translateY, translateZ);
        }

        float[] pivot = bone.getPivot();
        float pivotX = convertX(pivot[0]);
        float pivotY = convertY(pivot[1]);
        float pivotZ = convertZ(pivot[2]);

        stack.translate(pivotX, pivotY, pivotZ);

        float[] rotation = bone.getRotation();
        if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
            stack.rotateEuler(
                convertRotation(rotation[0], true),
                convertRotation(rotation[1], true),
                convertRotation(rotation[2], false)
            );
        }

        float[] size = bone.getSize();
        if (size[0] != 1 || size[1] != 1 || size[2] != 1) {
            stack.scale(size[0], size[1], size[2]);
        }

        stack.translate(-pivotX, -pivotY, -pivotZ);
    }

    private static void applyCubeTransform(ModelCube cube, CoreMatrixStack stack) {
        if (!cube.hasQuads()) {
            float[] origin = cube.getOrigin();
            stack.translate(convertX(origin[0]), convertY(origin[1]), convertZ(origin[2]));
        }

        if (cube.hasRotation()) {
            float[] pivot = cube.getPivot();
            float pivotX = convertX(pivot[0]);
            float pivotY = convertY(pivot[1]);
            float pivotZ = convertZ(pivot[2]);

            stack.translate(pivotX, pivotY, pivotZ);
            float[] rotation = cube.getRotation();
            stack.rotateEuler(
                convertRotation(rotation[0], true),
                convertRotation(rotation[1], true),
                convertRotation(rotation[2], false)
            );
            stack.translate(-pivotX, -pivotY, -pivotZ);
        }
    }

    private static void applyConstraints(Model model) {
        for (Constraint constraint : model.getConstraints()) {
            ModelBone target = model.getBone(constraint.getTargetBone());
            ModelBone source = model.getBone(constraint.getSourceBone());
            if (target != null && source != null) {
                constraint.apply(target, source);
            }
        }
    }

    private static float resolveTextureSize(String raw, float fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float convertX(float raw) {
        return -raw * PIXEL_SCALE;
    }

    private static float convertY(float raw) {
        return raw * PIXEL_SCALE;
    }

    private static float convertZ(float raw) {
        return raw * PIXEL_SCALE;
    }

    private static float convertRotation(float raw, boolean invert) {
        return invert ? -raw : raw;
    }

    /**
     * 渲染配置。
     */
    public static final class RenderOptions {
        private final CoreMatrixStack matrixStack;
        private final boolean applyConstraints;
        private final float textureWidth;
        private final float textureHeight;

        private RenderOptions(Builder builder) {
            this.matrixStack = builder.matrixStack;
            this.applyConstraints = builder.applyConstraints;
            this.textureWidth = builder.textureWidth;
            this.textureHeight = builder.textureHeight;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private CoreMatrixStack matrixStack;
            private boolean applyConstraints;
            private float textureWidth;
            private float textureHeight;

            public Builder matrixStack(CoreMatrixStack matrixStack) {
                this.matrixStack = matrixStack;
                return this;
            }

            public Builder applyConstraints(boolean applyConstraints) {
                this.applyConstraints = applyConstraints;
                return this;
            }

            public Builder textureWidth(float textureWidth) {
                this.textureWidth = textureWidth;
                return this;
            }

            public Builder textureHeight(float textureHeight) {
                this.textureHeight = textureHeight;
                return this;
            }

            public RenderOptions build() {
                return new RenderOptions(this);
            }
        }
    }
}
