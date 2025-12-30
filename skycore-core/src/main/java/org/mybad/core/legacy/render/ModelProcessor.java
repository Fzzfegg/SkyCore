package org.mybad.core.legacy.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.render.CoreMatrixStack;

/**
 * 渲染访问者接口，参照 Chameleon 的访问者模式并结合 HammerAnimations 的可扩展性。
 * 实现类可以在骨骼遍历和立方体渲染的不同阶段插入自定义逻辑。
 */
public interface ModelProcessor {

    /**
     * 在整个模型渲染开始时回调。
     */
    default void onStart(Model model, CoreMatrixStack stack) {
    }

    /**
     * 每个骨骼在应用变换后回调。
     * 返回 {@code true} 表示继续渲染该骨骼的子节点，返回 {@code false} 则跳过整个分支。
     */
    default boolean beforeBone(Model model, ModelBone bone, CoreMatrixStack stack) {
        return true;
    }

    /**
     * 立方体渲染回调，提供当前的纹理尺寸与矩阵堆栈。
     */
    default void renderCube(Model model,
                             ModelBone bone,
                             ModelCube cube,
                             float textureWidth,
                             float textureHeight,
                             CoreMatrixStack stack) {
    }

    /**
     * 骨骼渲染结束时回调。
     */
    default void afterBone(Model model, ModelBone bone, CoreMatrixStack stack) {
    }

    /**
     * 整个渲染流程结束时回调。
     */
    default void onFinish(Model model, CoreMatrixStack stack) {
    }
}
