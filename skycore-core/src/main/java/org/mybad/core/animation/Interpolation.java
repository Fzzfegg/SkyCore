package org.mybad.core.animation;

/**
 * 插值接口
 * 定义缓动函数用于动画关键帧间的平滑过渡
 */
public interface Interpolation {
    /**
     * 计算插值值
     * @param t 时间比例 [0, 1]，0表示起点，1表示终点
     * @return 插值系数 [0, 1]，控制过渡速度
     */
    float interpolate(float t);

    /**
     * 获取插值模式名称
     */
    String getName();
}
