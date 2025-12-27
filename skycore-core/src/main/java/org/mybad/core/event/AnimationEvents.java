package org.mybad.core.event;

import org.mybad.core.animation.*;

/**
 * 动画相关事件
 */
public class AnimationEvents {
    public static final String ANIMATION_STARTED = "animation.started";
    public static final String ANIMATION_PLAYING = "animation.playing";
    public static final String ANIMATION_PAUSED = "animation.paused";
    public static final String ANIMATION_STOPPED = "animation.stopped";
    public static final String ANIMATION_COMPLETED = "animation.completed";
    public static final String KEYFRAME_REACHED = "keyframe.reached";

    /**
     * 动画开始事件
     */
    public static class AnimationStartedEvent extends Event {
        private AnimationPlayer player;

        public AnimationStartedEvent(AnimationPlayer player) {
            super(ANIMATION_STARTED);
            this.player = player;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }
    }

    /**
     * 动画播放中事件
     */
    public static class AnimationPlayingEvent extends Event {
        private AnimationPlayer player;
        private float progress;

        public AnimationPlayingEvent(AnimationPlayer player, float progress) {
            super(ANIMATION_PLAYING);
            this.player = player;
            this.progress = progress;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }

        public float getProgress() {
            return progress;
        }
    }

    /**
     * 动画暂停事件
     */
    public static class AnimationPausedEvent extends Event {
        private AnimationPlayer player;

        public AnimationPausedEvent(AnimationPlayer player) {
            super(ANIMATION_PAUSED);
            this.player = player;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }
    }

    /**
     * 动画停止事件
     */
    public static class AnimationStoppedEvent extends Event {
        private AnimationPlayer player;

        public AnimationStoppedEvent(AnimationPlayer player) {
            super(ANIMATION_STOPPED);
            this.player = player;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }
    }

    /**
     * 动画完成事件
     */
    public static class AnimationCompletedEvent extends Event {
        private AnimationPlayer player;
        private int loopCount;

        public AnimationCompletedEvent(AnimationPlayer player, int loopCount) {
            super(ANIMATION_COMPLETED);
            this.player = player;
            this.loopCount = loopCount;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }

        public int getLoopCount() {
            return loopCount;
        }
    }

    /**
     * 关键帧到达事件
     */
    public static class KeyframeReachedEvent extends Event {
        private AnimationPlayer player;
        private String boneName;
        private float timestamp;

        public KeyframeReachedEvent(AnimationPlayer player, String boneName, float timestamp) {
            super(KEYFRAME_REACHED);
            this.player = player;
            this.boneName = boneName;
            this.timestamp = timestamp;
        }

        public AnimationPlayer getPlayer() {
            return player;
        }

        public String getBoneName() {
            return boneName;
        }

        public long getTimestamp() {
            return (long) timestamp;
        }
    }
}
