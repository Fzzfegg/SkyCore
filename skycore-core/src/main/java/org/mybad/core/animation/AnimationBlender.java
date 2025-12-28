package org.mybad.core.animation;

import org.mybad.core.data.*;
import java.util.*;

/**
 * 动画混合器
 * 支持多层动画的混合播放
 *
 * 特性：
 * - 支持多个动画同时播放
 * - 每个动画有独立的权重
 * - 支持过渡淡入淡出
 * - 动画优先级（后加的动画优先级更高）
 */
public class AnimationBlender {
    /**
     * 动画层信息
     */
    private static class AnimationLayer {
        AnimationPlayer player;
        float weight;
        float targetWeight;
        float transitionSpeed;
        boolean transitioning;
        int priority;

        AnimationLayer(AnimationPlayer player, int priority) {
            this.player = player;
            this.weight = 0;
            this.targetWeight = 0;
            this.transitionSpeed = 1.0f;
            this.transitioning = false;
            this.priority = priority;
        }
    }

    private List<AnimationLayer> layers;
    private Model model;

    public AnimationBlender(Model model) {
        this.model = model;
        this.layers = new ArrayList<>();
    }

    /**
     * 添加动画
     */
    public void addAnimation(AnimationPlayer player, float weight) {
        addAnimation(player, weight, layers.size());
    }

    /**
     * 添加动画（指定优先级）
     */
    public void addAnimation(AnimationPlayer player, float weight, int priority) {
        AnimationLayer layer = new AnimationLayer(player, priority);
        layer.targetWeight = Math.max(0, Math.min(1, weight));
        layer.weight = layer.targetWeight;
        layers.add(layer);

        // 按优先级排序
        layers.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    /**
     * 移除动画
     */
    public void removeAnimation(AnimationPlayer player) {
        layers.removeIf(layer -> layer.player == player);
    }

    /**
     * 清空所有动画
     */
    public void clear() {
        layers.clear();
    }

    /**
     * 设置动画权重
     */
    public void setWeight(AnimationPlayer player, float weight) {
        weight = Math.max(0, Math.min(1, weight));
        for (AnimationLayer layer : layers) {
            if (layer.player == player) {
                layer.weight = weight;           // 立即应用权重
                layer.targetWeight = weight;     // 也更新目标权重
                layer.transitioning = false;      // 停止过渡
                break;
            }
        }
    }

    /**
     * 淡入动画（过渡）
     */
    public void fadeIn(AnimationPlayer player, float duration) {
        for (AnimationLayer layer : layers) {
            if (layer.player == player) {
                layer.targetWeight = 1.0f;
                if (duration > 0) {
                    layer.transitionSpeed = 1.0f / duration;
                    layer.transitioning = true;
                } else {
                    layer.weight = 1.0f;
                    layer.transitioning = false;
                }
                break;
            }
        }
    }

    /**
     * 淡出动画（过渡）
     */
    public void fadeOut(AnimationPlayer player, float duration) {
        for (AnimationLayer layer : layers) {
            if (layer.player == player) {
                layer.targetWeight = 0.0f;
                if (duration > 0) {
                    layer.transitionSpeed = 1.0f / duration;
                    layer.transitioning = true;
                } else {
                    layer.weight = 0.0f;
                    layer.transitioning = false;
                }
                break;
            }
        }
    }

    /**
     * 更新所有动画
     */
    public void update(float deltaTime) {
        // 更新所有动画播放器
        for (AnimationLayer layer : layers) {
            layer.player.update(deltaTime);
        }

        // 更新权重过渡
        for (AnimationLayer layer : layers) {
            if (layer.transitioning) {
                float delta = layer.transitionSpeed * deltaTime;
                if (layer.weight < layer.targetWeight) {
                    layer.weight = Math.min(layer.targetWeight, layer.weight + delta);
                } else {
                    layer.weight = Math.max(layer.targetWeight, layer.weight - delta);
                }

                if (Math.abs(layer.weight - layer.targetWeight) < 0.001f) {
                    layer.weight = layer.targetWeight;
                    layer.transitioning = false;
                }
            }
        }

        // 注意：权重规范化已移除，允许任意权重值
        // 权重混合不需要总和为1，直接支持任意权重组合
    }

    /**
     * 应用所有动画到模型
     */
    public void apply() {
        if (model == null || layers.isEmpty()) {
            return;
        }

        model.resetToBindPose();

        // 逐层应用动画
        for (AnimationLayer layer : layers) {
            if (layer.weight > 0.001f) {  // 只应用权重足够大的动画
                layer.player.apply(model, layer.weight);
            }
        }
    }

    /**
     * 规范化权重
     * 使所有权重之和为1.0
     */
    private void normalizeWeights() {
        float totalWeight = 0;
        for (AnimationLayer layer : layers) {
            totalWeight += layer.weight;
        }

        if (totalWeight > 0.001f) {
            for (AnimationLayer layer : layers) {
                layer.weight /= totalWeight;
            }
        }
    }

    /**
     * 获取动画数量
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * 获取特定层的权重
     */
    public float getWeight(AnimationPlayer player) {
        for (AnimationLayer layer : layers) {
            if (layer.player == player) {
                return layer.weight;
            }
        }
        return 0;
    }

    /**
     * 检查是否包含动画
     */
    public boolean contains(AnimationPlayer player) {
        for (AnimationLayer layer : layers) {
            if (layer.player == player) {
                return true;
            }
        }
        return false;
    }

    /**
     * 播放所有动画
     */
    public void playAll() {
        for (AnimationLayer layer : layers) {
            layer.player.play();
        }
    }

    /**
     * 暂停所有动画
     */
    public void pauseAll() {
        for (AnimationLayer layer : layers) {
            layer.player.pause();
        }
    }

    /**
     * 停止所有动画
     */
    public void stopAll() {
        for (AnimationLayer layer : layers) {
            layer.player.stop();
        }
    }

    /**
     * 获取混合后的进度（0-1）
     */
    public float getBlendedProgress() {
        float totalProgress = 0;
        float totalWeight = 0;

        for (AnimationLayer layer : layers) {
            float progress = layer.player.getProgress();
            totalProgress += progress * layer.weight;
            totalWeight += layer.weight;
        }

        return totalWeight > 0 ? totalProgress / totalWeight : 0;
    }

    /**
     * 检查是否所有动画都播放完成
     */
    public boolean isAllFinished() {
        for (AnimationLayer layer : layers) {
            if (!layer.player.isFinished() && layer.weight > 0.001f) {
                return false;
            }
        }
        return true;
    }
}
