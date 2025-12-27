package org.mybad.minecraft.render;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 矩阵栈
 * 同时维护模型矩阵（4x4）和法线矩阵（3x3）
 *
 * 参考 Chameleon 的 MatrixStack 和 HammerAnimations 的 PoseStack
 * 优化：
 * - 预分配临时矩阵，避免 GC
 * - 支持与 OpenGL 状态交互
 */
@SideOnly(Side.CLIENT)
public class MatrixStack {

    /** 矩阵栈 */
    private final Deque<Entry> stack = new ArrayDeque<>();

    /** 预分配的临时矩阵，避免每次操作创建新对象 (Chameleon 风格) */
    private final Matrix4f tempModelMatrix = new Matrix4f();
    private final Matrix3f tempNormalMatrix = new Matrix3f();

    /** 预分配的临时向量 */
    private final Vector3f tempVec3 = new Vector3f();
    private final Vector4f tempVec4 = new Vector4f();

    /** OpenGL 矩阵缓冲 (HammerAnimations 风格) */
    private static final FloatBuffer GL_MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    /** 度数到弧度转换因子 */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public MatrixStack() {
        // 初始化为单位矩阵
        Entry entry = new Entry();
        entry.pose.setIdentity();
        entry.normal.setIdentity();
        stack.addLast(entry);
    }

    /**
     * 获取当前模型矩阵
     */
    public Matrix4f getModelMatrix() {
        return stack.getLast().pose;
    }

    /**
     * 获取当前法线矩阵
     */
    public Matrix3f getNormalMatrix() {
        return stack.getLast().normal;
    }

    /**
     * 获取当前条目
     */
    public Entry last() {
        return stack.getLast();
    }

    /**
     * 压栈（保存当前状态）
     */
    public void push() {
        Entry current = stack.getLast();
        Entry copy = new Entry();
        copy.pose.set(current.pose);
        copy.normal.set(current.normal);
        stack.addLast(copy);
    }

    /**
     * 出栈（恢复上一个状态）
     */
    public void pop() {
        if (stack.size() > 1) {
            stack.removeLast();
        }
    }

    /**
     * 重置为单位矩阵
     */
    public void reset() {
        stack.clear();
        Entry entry = new Entry();
        entry.pose.setIdentity();
        entry.normal.setIdentity();
        stack.addLast(entry);
    }

    /**
     * 设置当前为单位矩阵 (HammerAnimations 风格)
     */
    public void setIdentity() {
        Entry last = stack.getLast();
        last.pose.setIdentity();
        last.normal.setIdentity();
    }

    // ==================== 变换操作 ====================

    /**
     * 平移 (Chameleon 风格 - 使用预分配临时矩阵)
     */
    public void translate(float x, float y, float z) {
        tempVec3.set(x, y, z);
        tempModelMatrix.setIdentity();
        tempModelMatrix.setTranslation(tempVec3);
        stack.getLast().pose.mul(tempModelMatrix);
        // 平移不影响法线矩阵
    }

    /**
     * 平移到立方体旋转中心 (Chameleon 风格)
     */
    public void moveToCubePivot(float[] pivot) {
        translate(pivot[0] / 16F, pivot[1] / 16F, pivot[2] / 16F);
    }

    /**
     * 从立方体旋转中心移回 (Chameleon 风格)
     */
    public void moveBackFromCubePivot(float[] pivot) {
        translate(-pivot[0] / 16F, -pivot[1] / 16F, -pivot[2] / 16F);
    }

    /**
     * 平移到骨骼旋转中心 (Chameleon 风格)
     */
    public void moveToBonePivot(float[] pivot) {
        translate(pivot[0] / 16F, pivot[1] / 16F, pivot[2] / 16F);
    }

    /**
     * 从骨骼旋转中心移回 (Chameleon 风格)
     */
    public void moveBackFromBonePivot(float[] pivot) {
        translate(-pivot[0] / 16F, -pivot[1] / 16F, -pivot[2] / 16F);
    }

    /**
     * 骨骼位移 (Chameleon translateBone 风格)
     * position 是动画产生的偏移，pivot 是骨骼的初始位置
     */
    public void translateBone(float[] position, float[] pivot) {
        translate(
                -(position[0] - pivot[0]) / 16F,
                (position[1] - pivot[1]) / 16F,
                (position[2] - pivot[2]) / 16F
        );
    }

    /**
     * 绕 X 轴旋转（弧度）(Chameleon 风格)
     */
    public void rotateX(float radian) {
        tempModelMatrix.setIdentity();
        tempModelMatrix.rotX(radian);

        tempNormalMatrix.setIdentity();
        tempNormalMatrix.rotX(radian);

        stack.getLast().pose.mul(tempModelMatrix);
        stack.getLast().normal.mul(tempNormalMatrix);
    }

    /**
     * 绕 Y 轴旋转（弧度）
     */
    public void rotateY(float radian) {
        tempModelMatrix.setIdentity();
        tempModelMatrix.rotY(radian);

        tempNormalMatrix.setIdentity();
        tempNormalMatrix.rotY(radian);

        stack.getLast().pose.mul(tempModelMatrix);
        stack.getLast().normal.mul(tempNormalMatrix);
    }

    /**
     * 绕 Z 轴旋转（弧度）
     */
    public void rotateZ(float radian) {
        tempModelMatrix.setIdentity();
        tempModelMatrix.rotZ(radian);

        tempNormalMatrix.setIdentity();
        tempNormalMatrix.rotZ(radian);

        stack.getLast().pose.mul(tempModelMatrix);
        stack.getLast().normal.mul(tempNormalMatrix);
    }

    /**
     * 绕 X 轴旋转（度数）
     */
    public void rotateXDeg(float degrees) {
        if (degrees != 0) rotateX(degrees * DEG_TO_RAD);
    }

    /**
     * 绕 Y 轴旋转（度数）
     */
    public void rotateYDeg(float degrees) {
        if (degrees != 0) rotateY(degrees * DEG_TO_RAD);
    }

    /**
     * 绕 Z 轴旋转（度数）
     */
    public void rotateZDeg(float degrees) {
        if (degrees != 0) rotateZ(degrees * DEG_TO_RAD);
    }

    /**
     * ZYX 顺序旋转（度数）- Bedrock 标准 (Chameleon rotateBone 风格)
     */
    public void rotateZYX(float rx, float ry, float rz) {
        if (rz != 0) rotateZ(rz * DEG_TO_RAD);
        if (ry != 0) rotateY(ry * DEG_TO_RAD);
        if (rx != 0) rotateX(rx * DEG_TO_RAD);
    }

    /**
     * 立方体旋转 (Chameleon rotateCube 风格 - 合并为单次矩阵乘法)
     */
    public void rotateCube(float[] rotation) {
        if (rotation[0] == 0 && rotation[1] == 0 && rotation[2] == 0) {
            return;
        }

        Matrix4f rot4 = new Matrix4f();
        Matrix3f rot3 = new Matrix3f();

        // 构建组合旋转矩阵
        tempModelMatrix.setIdentity();
        rot4.rotZ(rotation[2] * DEG_TO_RAD);
        tempModelMatrix.mul(rot4);
        rot4.rotY(rotation[1] * DEG_TO_RAD);
        tempModelMatrix.mul(rot4);
        rot4.rotX(rotation[0] * DEG_TO_RAD);
        tempModelMatrix.mul(rot4);

        tempNormalMatrix.setIdentity();
        rot3.rotZ(rotation[2] * DEG_TO_RAD);
        tempNormalMatrix.mul(rot3);
        rot3.rotY(rotation[1] * DEG_TO_RAD);
        tempNormalMatrix.mul(rot3);
        rot3.rotX(rotation[0] * DEG_TO_RAD);
        tempNormalMatrix.mul(rot3);

        stack.getLast().pose.mul(tempModelMatrix);
        stack.getLast().normal.mul(tempNormalMatrix);
    }

    /**
     * 缩放 (Chameleon 风格 - 正确处理法线)
     */
    public void scale(float x, float y, float z) {
        tempModelMatrix.setIdentity();
        tempModelMatrix.m00 = x;
        tempModelMatrix.m11 = y;
        tempModelMatrix.m22 = z;

        stack.getLast().pose.mul(tempModelMatrix);

        // 法线矩阵：只在有负缩放时需要更新 (Chameleon 优化)
        if (x < 0 || y < 0 || z < 0) {
            tempNormalMatrix.setIdentity();
            tempNormalMatrix.m00 = x < 0 ? -1 : 1;
            tempNormalMatrix.m11 = y < 0 ? -1 : 1;
            tempNormalMatrix.m22 = z < 0 ? -1 : 1;
            stack.getLast().normal.mul(tempNormalMatrix);
        }
    }

    /**
     * 均匀缩放
     */
    public void scale(float s) {
        scale(s, s, s);
    }

    /**
     * 骨骼缩放 (Chameleon scaleBone 风格)
     */
    public void scaleBone(float[] boneScale) {
        scale(boneScale[0], boneScale[1], boneScale[2]);
    }

    // ==================== 顶点变换 ====================

    /**
     * 变换顶点位置（使用预分配向量）
     */
    public void transformPosition(float x, float y, float z, float[] out) {
        tempVec4.set(x, y, z, 1.0f);
        getModelMatrix().transform(tempVec4);
        out[0] = tempVec4.x;
        out[1] = tempVec4.y;
        out[2] = tempVec4.z;
    }

    /**
     * 变换法线方向（使用预分配向量）
     */
    public void transformNormal(float nx, float ny, float nz, float[] out) {
        tempVec3.set(nx, ny, nz);
        getNormalMatrix().transform(tempVec3);

        // 归一化
        float len = (float) Math.sqrt(tempVec3.x * tempVec3.x + tempVec3.y * tempVec3.y + tempVec3.z * tempVec3.z);
        if (len > 0.0001f) {
            out[0] = tempVec3.x / len;
            out[1] = tempVec3.y / len;
            out[2] = tempVec3.z / len;
        } else {
            out[0] = nx;
            out[1] = ny;
            out[2] = nz;
        }
    }

    // ==================== OpenGL 交互 (HammerAnimations 风格) ====================

    /**
     * 从当前 OpenGL 模型视图矩阵加载
     */
    public MatrixStack fromGL() {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, GL_MATRIX_BUFFER);

        // 读取 OpenGL 矩阵（列优先）
        Matrix4f glMatrix = new Matrix4f();
        glMatrix.m00 = GL_MATRIX_BUFFER.get(0);
        glMatrix.m10 = GL_MATRIX_BUFFER.get(1);
        glMatrix.m20 = GL_MATRIX_BUFFER.get(2);
        glMatrix.m30 = GL_MATRIX_BUFFER.get(3);
        glMatrix.m01 = GL_MATRIX_BUFFER.get(4);
        glMatrix.m11 = GL_MATRIX_BUFFER.get(5);
        glMatrix.m21 = GL_MATRIX_BUFFER.get(6);
        glMatrix.m31 = GL_MATRIX_BUFFER.get(7);
        glMatrix.m02 = GL_MATRIX_BUFFER.get(8);
        glMatrix.m12 = GL_MATRIX_BUFFER.get(9);
        glMatrix.m22 = GL_MATRIX_BUFFER.get(10);
        glMatrix.m32 = GL_MATRIX_BUFFER.get(11);
        glMatrix.m03 = GL_MATRIX_BUFFER.get(12);
        glMatrix.m13 = GL_MATRIX_BUFFER.get(13);
        glMatrix.m23 = GL_MATRIX_BUFFER.get(14);
        glMatrix.m33 = GL_MATRIX_BUFFER.get(15);

        GL_MATRIX_BUFFER.rewind();

        // 计算法线矩阵 = transpose(inverse(modelMatrix 3x3部分))
        Matrix3f normalMatrix = new Matrix3f();
        glMatrix.getRotationScale(normalMatrix);
        normalMatrix.transpose();
        normalMatrix.invert();

        Entry last = stack.getLast();
        last.pose.mul(glMatrix);
        last.normal.mul(normalMatrix);

        return this;
    }

    /**
     * 将当前矩阵加载到 OpenGL
     */
    public void toGL() {
        Matrix4f pose = getModelMatrix();

        // 写入 OpenGL 矩阵（列优先）
        GL_MATRIX_BUFFER.put(0, pose.m00);
        GL_MATRIX_BUFFER.put(1, pose.m10);
        GL_MATRIX_BUFFER.put(2, pose.m20);
        GL_MATRIX_BUFFER.put(3, pose.m30);
        GL_MATRIX_BUFFER.put(4, pose.m01);
        GL_MATRIX_BUFFER.put(5, pose.m11);
        GL_MATRIX_BUFFER.put(6, pose.m21);
        GL_MATRIX_BUFFER.put(7, pose.m31);
        GL_MATRIX_BUFFER.put(8, pose.m02);
        GL_MATRIX_BUFFER.put(9, pose.m12);
        GL_MATRIX_BUFFER.put(10, pose.m22);
        GL_MATRIX_BUFFER.put(11, pose.m32);
        GL_MATRIX_BUFFER.put(12, pose.m03);
        GL_MATRIX_BUFFER.put(13, pose.m13);
        GL_MATRIX_BUFFER.put(14, pose.m23);
        GL_MATRIX_BUFFER.put(15, pose.m33);

        GL11.glLoadMatrix(GL_MATRIX_BUFFER);
    }

    /**
     * 获取栈深度
     */
    public int getDepth() {
        return stack.size();
    }

    /**
     * 矩阵栈条目 (HammerAnimations Entry 风格)
     */
    public static class Entry {
        public final Matrix4f pose = new Matrix4f();
        public final Matrix3f normal = new Matrix3f();

        public Matrix4f getPose() {
            return pose;
        }

        public Matrix3f getNormal() {
            return normal;
        }
    }
}
