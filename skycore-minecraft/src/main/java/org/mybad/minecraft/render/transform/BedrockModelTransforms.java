package org.mybad.minecraft.render.transform;

import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;

/**
 * Shared Bedrock model coordinate conversions and bone/cube transforms.
 */
public final class BedrockModelTransforms {
    private static final float PIXEL_SCALE = 1.0f / 16.0f;

    private BedrockModelTransforms() {
    }

    public static float convertX(float raw) {
        return -raw * PIXEL_SCALE;
    }

    public static float convertY(float raw) {
        return raw * PIXEL_SCALE;
    }

    public static float convertZ(float raw) {
        return raw * PIXEL_SCALE;
    }

    public static float convertLocatorX(float raw) {
        return -raw * PIXEL_SCALE;
    }

    public static float convertLocatorY(float raw) {
        return raw * PIXEL_SCALE;
    }

    public static float convertLocatorZ(float raw) {
        return raw * PIXEL_SCALE;
    }

    public static float convertRotation(float raw, boolean invert) {
        return invert ? -raw : raw;
    }

    public static void applyBoneTransform(ModelBone bone, MatrixStack stack) {
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

    public static void applyBoneTransformRecursive(ModelBone bone, MatrixStack stack) {
        if (bone.getParent() != null) {
            applyBoneTransformRecursive(bone.getParent(), stack);
        }
        applyBoneTransform(bone, stack);
    }

    public static MatrixStack buildCubeRotationStack(ModelCube cube) {
        if (!cube.hasRotation()) {
            return null;
        }
        MatrixStack stack = new MatrixStack();
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
        return stack;
    }
}
