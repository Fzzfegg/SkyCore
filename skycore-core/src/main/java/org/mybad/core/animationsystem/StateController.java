package org.mybad.core.animationsystem;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;

/**
 * 状态控制器
 * 管理单个动画状态的生命周期
 * 支持动画关联、超时机制、回调系统
 */
public class StateController {
    private String stateName;
    private long entryTime;
    private float stateTimer;
    private AnimationPlayer player;
    private Animation animation;
    private float timeout;  // -1 表示无限
    private boolean timedOut;

    // 回调函数
    private Runnable onEnterCallback;
    private Runnable onExitCallback;
    private Runnable onUpdateCallback;
    private Runnable onTimeoutCallback;

    // 构造函数

    /**
     * 创建状态控制器
     * @param stateName 状态名称
     */
    public StateController(String stateName) {
        this.stateName = stateName;
        this.timeout = -1.0f;
        this.timedOut = false;
        this.stateTimer = 0.0f;
        this.entryTime = System.currentTimeMillis();
    }

    /**
     * 创建状态控制器并关联动画
     * @param stateName 状态名称
     * @param animation 关联的动画
     */
    public StateController(String stateName, Animation animation) {
        this(stateName);
        this.animation = animation;
    }

    // 生命周期方法

    /**
     * 进入状态
     */
    public void enter() {
        entryTime = System.currentTimeMillis();
        stateTimer = 0.0f;
        timedOut = false;

        // 启动动画
        if (animation != null && player == null) {
            player = new AnimationPlayer(animation);
            player.play();
        } else if (player != null) {
            player.play();
        }

        // 触发回调
        if (onEnterCallback != null) {
            onEnterCallback.run();
        }
    }

    /**
     * 更新状态
     * @param deltaTime 帧时间（秒）
     */
    public void update(float deltaTime) {
        // 更新动画播放器
        if (player != null && player.getState().isPlaying()) {
            player.update(deltaTime);
        }

        // 更新计时器
        stateTimer += deltaTime;

        // 检查超时
        if (timeout > 0 && stateTimer >= timeout && !timedOut) {
            timedOut = true;
            if (onTimeoutCallback != null) {
                onTimeoutCallback.run();
            }
        }

        // 触发更新回调
        if (onUpdateCallback != null) {
            onUpdateCallback.run();
        }
    }

    /**
     * 退出状态
     */
    public void exit() {
        // 停止动画
        if (player != null) {
            player.stop();
        }

        // 触发回调
        if (onExitCallback != null) {
            onExitCallback.run();
        }
    }

    // 动画控制方法

    /**
     * 设置关联的动画
     */
    public void setAnimation(Animation animation) {
        this.animation = animation;
        if (player == null && animation != null) {
            this.player = new AnimationPlayer(animation);
        } else if (player != null) {
            this.player = new AnimationPlayer(animation);
        }
    }

    /**
     * 获取动画播放器
     */
    public AnimationPlayer getAnimationPlayer() {
        return player;
    }

    /**
     * 播放动画
     */
    public void playAnimation() {
        if (player != null) {
            player.play();
        }
    }

    /**
     * 停止动画
     */
    public void stopAnimation() {
        if (player != null) {
            player.stop();
        }
    }

    /**
     * 暂停动画
     */
    public void pauseAnimation() {
        if (player != null) {
            player.pause();
        }
    }

    /**
     * 恢复动画
     */
    public void resumeAnimation() {
        if (player != null) {
            player.resume();
        }
    }

    // 超时处理方法

    /**
     * 设置状态超时时间
     * @param seconds 超时秒数，-1表示无限
     */
    public void setTimeout(float seconds) {
        this.timeout = seconds;
    }

    /**
     * 是否已超时
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * 获取超时时间
     */
    public float getTimeout() {
        return timeout;
    }

    // 回调设置方法

    /**
     * 设置进入回调
     */
    public void setOnEnter(Runnable callback) {
        this.onEnterCallback = callback;
    }

    /**
     * 设置退出回调
     */
    public void setOnExit(Runnable callback) {
        this.onExitCallback = callback;
    }

    /**
     * 设置更新回调
     */
    public void setOnUpdate(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    /**
     * 设置超时回调
     */
    public void setOnTimeout(Runnable callback) {
        this.onTimeoutCallback = callback;
    }

    // Getter 方法

    public String getStateName() {
        return stateName;
    }

    public float getStateTimer() {
        return stateTimer;
    }

    public Animation getAnimation() {
        return animation;
    }

    public long getEntryTime() {
        return entryTime;
    }

    @Override
    public String toString() {
        return String.format("StateController[%s, time=%.2f, timedOut=%s]", stateName, stateTimer, timedOut);
    }
}
