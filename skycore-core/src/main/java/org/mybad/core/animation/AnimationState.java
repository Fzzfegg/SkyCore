package org.mybad.core.animation;

/**
 * 动画播放状态
 * 跟踪动画的播放进度和状态
 */
public class AnimationState {
    public enum PlayState {
        STOPPED,    // 停止
        PLAYING,    // 播放中
        PAUSED      // 暂停
    }

    private Animation animation;
    private float currentTime;      // 当前播放时间
    private PlayState state;        // 播放状态
    private int loopCount;          // 循环次数
    private boolean finished;       // 是否播放完成

    public AnimationState(Animation animation) {
        this.animation = animation;
        this.currentTime = 0;
        this.state = PlayState.STOPPED;
        this.loopCount = 0;
        this.finished = false;
    }

    /**
     * 更新动画状态
     * @param deltaTime 时间增量（秒）
     */
    public void update(float deltaTime) {
        if (state != PlayState.PLAYING || animation == null) {
            return;
        }

        // 应用速度倍数
        float actualDelta = deltaTime * animation.getSpeed();
        currentTime += actualDelta;

        // 处理动画长度
        float length = animation.getLength();
        if (length <= 0f) {
            currentTime = 0f;
            if (!animation.isLoop()) {
                state = PlayState.STOPPED;
                finished = true;
            }
            return;
        }

        if (currentTime >= length) {
            if (animation.getLoopMode() == Animation.LoopMode.LOOP) {
                // 循环
                loopCount++;
                currentTime = currentTime % length;
            } else {
                // 停止（包含 HOLD_ON_LAST_FRAME）
                currentTime = length;
                state = PlayState.STOPPED;
                finished = true;
            }
        }
    }

    /**
     * 播放动画
     */
    public void play() {
        state = PlayState.PLAYING;
        finished = false;
    }

    /**
     * 暂停动画
     */
    public void pause() {
        if (state == PlayState.PLAYING) {
            state = PlayState.PAUSED;
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        if (state == PlayState.PAUSED) {
            state = PlayState.PLAYING;
        }
    }

    /**
     * 停止动画
     */
    public void stop() {
        state = PlayState.STOPPED;
        currentTime = 0;
        loopCount = 0;
        finished = false;
    }

    /**
     * 重新开始动画
     */
    public void restart() {
        stop();
        play();
    }

    /**
     * 设置当前时间
     */
    public void setCurrentTime(float time) {
        this.currentTime = Math.max(0, Math.min(time, animation.getLength()));
    }

    /**
     * 获取当前进度（0-1）
     */
    public float getProgress() {
        if (animation.getLength() <= 0) {
            return 0;
        }
        return currentTime / animation.getLength();
    }

    /**
     * 获取规范化的时间（0-1范围内的循环时间）
     */
    public float getNormalizedTime() {
        if (animation.getLength() <= 0) {
            return 0;
        }
        return (currentTime % animation.getLength()) / animation.getLength();
    }

    // Getters
    public Animation getAnimation() { return animation; }
    public float getCurrentTime() { return currentTime; }
    public PlayState getState() { return state; }
    public int getLoopCount() { return loopCount; }
    public boolean isFinished() { return finished; }
    public boolean isPlaying() { return state == PlayState.PLAYING; }
    public boolean isPaused() { return state == PlayState.PAUSED; }
    public boolean isStopped() { return state == PlayState.STOPPED; }

    public boolean shouldApply() {
        if (state == PlayState.PLAYING) {
            return true;
        }
        return finished && animation != null && animation.isHoldOnLastFrame();
    }
}
