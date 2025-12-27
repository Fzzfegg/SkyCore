package org.mybad.core.utils;

/**
 * 变换工具库
 * 提供向量和矩阵操作的便利函数
 */
public class TransformUtils {

    /**
     * 欧拉角转四元数
     * 使用XYZ顺序
     */
    public static float[] eulerToQuaternion(float x, float y, float z) {
        // 转换为弧度
        x = (float) Math.toRadians(x);
        y = (float) Math.toRadians(y);
        z = (float) Math.toRadians(z);

        float cx = (float) Math.cos(x * 0.5f);
        float sx = (float) Math.sin(x * 0.5f);
        float cy = (float) Math.cos(y * 0.5f);
        float sy = (float) Math.sin(y * 0.5f);
        float cz = (float) Math.cos(z * 0.5f);
        float sz = (float) Math.sin(z * 0.5f);

        float[] q = new float[4];
        q[0] = sx * cy * cz - cx * sy * sz;  // x
        q[1] = cx * sy * cz + sx * cy * sz;  // y
        q[2] = cx * cy * sz - sx * sy * cz;  // z
        q[3] = cx * cy * cz + sx * sy * sz;  // w

        return q;
    }

    /**
     * 四元数转欧拉角
     * 返回角度（度数）的XYZ顺序
     */
    public static float[] quaternionToEuler(float x, float y, float z, float w) {
        float[] euler = new float[3];

        // Roll (X轴旋转)
        float sinr_cosp = 2 * (w * x + y * z);
        float cosr_cosp = 1 - 2 * (x * x + y * y);
        euler[0] = (float) Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp));

        // Pitch (Y轴旋转)
        float sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1) {
            euler[1] = (float) Math.copySign(90, sinp);
        } else {
            euler[1] = (float) Math.toDegrees(Math.asin(sinp));
        }

        // Yaw (Z轴旋转)
        float siny_cosp = 2 * (w * z + x * y);
        float cosy_cosp = 1 - 2 * (y * y + z * z);
        euler[2] = (float) Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp));

        return euler;
    }

    /**
     * 向量长度
     */
    public static float length(float[] v) {
        if (v.length < 3) {
            return 0;
        }
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    /**
     * 向量归一化
     */
    public static float[] normalize(float[] v) {
        if (v.length < 3) {
            return v;
        }

        float len = length(v);
        if (len <= 0) {
            return v;
        }

        float[] result = new float[3];
        result[0] = v[0] / len;
        result[1] = v[1] / len;
        result[2] = v[2] / len;

        return result;
    }

    /**
     * 向量点积
     */
    public static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * 向量叉积
     */
    public static float[] cross(float[] a, float[] b) {
        float[] result = new float[3];
        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];
        return result;
    }

    /**
     * 向量加法
     */
    public static float[] add(float[] a, float[] b) {
        float[] result = new float[3];
        result[0] = a[0] + b[0];
        result[1] = a[1] + b[1];
        result[2] = a[2] + b[2];
        return result;
    }

    /**
     * 向量减法
     */
    public static float[] subtract(float[] a, float[] b) {
        float[] result = new float[3];
        result[0] = a[0] - b[0];
        result[1] = a[1] - b[1];
        result[2] = a[2] - b[2];
        return result;
    }

    /**
     * 向量缩放
     */
    public static float[] scale(float[] v, float factor) {
        float[] result = new float[3];
        result[0] = v[0] * factor;
        result[1] = v[1] * factor;
        result[2] = v[2] * factor;
        return result;
    }

    /**
     * 线性插值
     */
    public static float[] lerp(float[] a, float[] b, float t) {
        float[] result = new float[3];
        result[0] = a[0] + (b[0] - a[0]) * t;
        result[1] = a[1] + (b[1] - a[1]) * t;
        result[2] = a[2] + (b[2] - a[2]) * t;
        return result;
    }

    /**
     * 四元数乘法
     */
    public static float[] multiplyQuaternion(float[] q1, float[] q2) {
        float[] result = new float[4];

        // q1 = [x1, y1, z1, w1], q2 = [x2, y2, z2, w2]
        result[0] = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1];
        result[1] = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0];
        result[2] = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3];
        result[3] = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2];

        return result;
    }

    /**
     * 四元数共轭
     */
    public static float[] conjugateQuaternion(float[] q) {
        return new float[]{-q[0], -q[1], -q[2], q[3]};
    }

    /**
     * 四元数反演（倒数）
     */
    public static float[] inverseQuaternion(float[] q) {
        float dotProduct = dot(q, q) + q[3] * q[3];
        if (dotProduct <= 0) {
            return new float[]{0, 0, 0, 1};
        }

        float[] conj = conjugateQuaternion(q);
        return scale(conj, 1.0f / dotProduct);
    }

    /**
     * 计算两向量间的夹角（度数）
     */
    public static float angleBetween(float[] a, float[] b) {
        float[] na = normalize(a);
        float[] nb = normalize(b);
        float dotProd = Math.max(-1.0f, Math.min(1.0f, dot(na, nb)));
        return (float) Math.toDegrees(Math.acos(dotProd));
    }

    /**
     * 计算距离
     */
    public static float distance(float[] a, float[] b) {
        return length(subtract(b, a));
    }

    /**
     * 插值两个角度（最短路径）
     */
    public static float lerpAngle(float a, float b, float t) {
        float delta = ((b - a + 180) % 360 + 360) % 360 - 180;
        return a + delta * t;
    }

    /**
     * 创建4x4变换矩阵
     * 组合平移、旋转和缩放变换
     * 矩阵存储在列主序（OpenGL风格）
     */
    public static void createMatrix(float[] matrix,
                                     float posX, float posY, float posZ,
                                     float rotX, float rotY, float rotZ,
                                     float scaleX, float scaleY, float scaleZ) {
        // 初始化为单位矩阵
        for (int i = 0; i < 16; i++) {
            matrix[i] = 0;
        }
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;

        // 应用缩放
        matrix[0] = scaleX;
        matrix[5] = scaleY;
        matrix[10] = scaleZ;

        // 转换为弧度
        rotX = (float) Math.toRadians(rotX);
        rotY = (float) Math.toRadians(rotY);
        rotZ = (float) Math.toRadians(rotZ);

        // 计算旋转矩阵（XYZ顺序）
        float cx = (float) Math.cos(rotX);
        float sx = (float) Math.sin(rotX);
        float cy = (float) Math.cos(rotY);
        float sy = (float) Math.sin(rotY);
        float cz = (float) Math.cos(rotZ);
        float sz = (float) Math.sin(rotZ);

        // 组合旋转和缩放
        matrix[0] = scaleX * (cy * cz);
        matrix[1] = scaleX * (cy * sz);
        matrix[2] = scaleX * (-sy);

        matrix[4] = scaleY * (sx * sy * cz - cx * sz);
        matrix[5] = scaleY * (sx * sy * sz + cx * cz);
        matrix[6] = scaleY * (sx * cy);

        matrix[8] = scaleZ * (cx * sy * cz + sx * sz);
        matrix[9] = scaleZ * (cx * sy * sz - sx * cz);
        matrix[10] = scaleZ * (cx * cy);

        // 应用平移
        matrix[12] = posX;
        matrix[13] = posY;
        matrix[14] = posZ;
        matrix[15] = 1;
    }
}
