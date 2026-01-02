package org.mybad.minecraft.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelLocator;

final class LocatorResolver {
    private LocatorResolver() {
    }

    static float[] getLocatorPosition(Model model, String locatorName) {
        if (locatorName == null || locatorName.isEmpty() || model == null) {
            return null;
        }
        ModelLocator locator = model.getLocator(locatorName);
        if (locator == null) {
            return null;
        }
        ConstraintApplier.apply(model);
        float[] raw = locator.getPosition();
        float[] local = new float[]{
            BedrockModelTransforms.convertLocatorX(raw[0]),
            BedrockModelTransforms.convertLocatorY(raw[1]),
            BedrockModelTransforms.convertLocatorZ(raw[2])
        };
        String boneName = locator.getAttachedBone();
        if (boneName == null || boneName.isEmpty()) {
            return local;
        }
        ModelBone bone = model.getBone(boneName);
        if (bone == null) {
            return local;
        }
        MatrixStack stack = new MatrixStack();
        BedrockModelTransforms.applyBoneTransformRecursive(bone, stack);
        stack.transform(local);
        return local;
    }

    static boolean getLocatorTransform(Model model, String locatorName, LocatorTransform out) {
        if (out == null || locatorName == null || locatorName.isEmpty() || model == null) {
            return false;
        }
        ModelLocator locator = model.getLocator(locatorName);
        if (locator == null) {
            return false;
        }
        ConstraintApplier.apply(model);
        float[] raw = locator.getPosition();
        float[] local = new float[]{
            BedrockModelTransforms.convertLocatorX(raw[0]),
            BedrockModelTransforms.convertLocatorY(raw[1]),
            BedrockModelTransforms.convertLocatorZ(raw[2])
        };
        String boneName = locator.getAttachedBone();
        MatrixStack stack = new MatrixStack();
        if (boneName != null && !boneName.isEmpty()) {
            ModelBone bone = model.getBone(boneName);
            if (bone != null) {
                BedrockModelTransforms.applyBoneTransformRecursive(bone, stack);
            }
        }
        stack.transform(local);
        out.position[0] = local[0];
        out.position[1] = local[1];
        out.position[2] = local[2];
        fillBasisFromMatrix(stack.getModelMatrix(), out);
        float[] locatorRot = locator.getRotation();
        if (locatorRot != null && (locatorRot[0] != 0 || locatorRot[1] != 0 || locatorRot[2] != 0)) {
            MatrixStack rotStack = new MatrixStack();
            rotStack.rotateEuler(
                BedrockModelTransforms.convertRotation(locatorRot[0], true),
                BedrockModelTransforms.convertRotation(locatorRot[1], true),
                BedrockModelTransforms.convertRotation(locatorRot[2], false)
            );
            applyRotationToBasis(rotStack.getModelMatrix(), out);
        }
        return true;
    }

    private static void fillBasisFromMatrix(javax.vecmath.Matrix4f matrix, LocatorTransform out) {
        javax.vecmath.Matrix3f rot = new javax.vecmath.Matrix3f();
        matrix.getRotationScale(rot);
        javax.vecmath.Vector3f xAxis = new javax.vecmath.Vector3f(1, 0, 0);
        javax.vecmath.Vector3f yAxis = new javax.vecmath.Vector3f(0, 1, 0);
        javax.vecmath.Vector3f zAxis = new javax.vecmath.Vector3f(0, 0, 1);
        rot.transform(xAxis);
        rot.transform(yAxis);
        rot.transform(zAxis);
        float scaleX = xAxis.length();
        float scaleY = yAxis.length();
        float scaleZ = zAxis.length();
        orthonormalize(xAxis, yAxis, zAxis);
        out.basisX[0] = xAxis.x;
        out.basisX[1] = xAxis.y;
        out.basisX[2] = xAxis.z;
        out.basisY[0] = yAxis.x;
        out.basisY[1] = yAxis.y;
        out.basisY[2] = yAxis.z;
        out.basisZ[0] = zAxis.x;
        out.basisZ[1] = zAxis.y;
        out.basisZ[2] = zAxis.z;
        out.scale[0] = scaleX == 0.0f ? 1.0f : scaleX;
        out.scale[1] = scaleY == 0.0f ? 1.0f : scaleY;
        out.scale[2] = scaleZ == 0.0f ? 1.0f : scaleZ;
    }

    private static void applyRotationToBasis(javax.vecmath.Matrix4f matrix, LocatorTransform out) {
        javax.vecmath.Matrix3f rot = new javax.vecmath.Matrix3f();
        matrix.getRotationScale(rot);
        javax.vecmath.Vector3f xAxis = new javax.vecmath.Vector3f(out.basisX[0], out.basisX[1], out.basisX[2]);
        javax.vecmath.Vector3f yAxis = new javax.vecmath.Vector3f(out.basisY[0], out.basisY[1], out.basisY[2]);
        javax.vecmath.Vector3f zAxis = new javax.vecmath.Vector3f(out.basisZ[0], out.basisZ[1], out.basisZ[2]);
        rot.transform(xAxis);
        rot.transform(yAxis);
        rot.transform(zAxis);
        orthonormalize(xAxis, yAxis, zAxis);
        out.basisX[0] = xAxis.x;
        out.basisX[1] = xAxis.y;
        out.basisX[2] = xAxis.z;
        out.basisY[0] = yAxis.x;
        out.basisY[1] = yAxis.y;
        out.basisY[2] = yAxis.z;
        out.basisZ[0] = zAxis.x;
        out.basisZ[1] = zAxis.y;
        out.basisZ[2] = zAxis.z;
    }

    private static void orthonormalize(javax.vecmath.Vector3f xAxis,
                                       javax.vecmath.Vector3f yAxis,
                                       javax.vecmath.Vector3f zAxis) {
        normalize(xAxis);
        float dot = xAxis.dot(yAxis);
        yAxis.x -= xAxis.x * dot;
        yAxis.y -= xAxis.y * dot;
        yAxis.z -= xAxis.z * dot;
        normalize(yAxis);
        zAxis.cross(xAxis, yAxis);
        normalize(zAxis);
    }

    private static void normalize(javax.vecmath.Vector3f axis) {
        float len = axis.length();
        if (len != 0.0f) {
            axis.scale(1.0f / len);
        }
    }
}
