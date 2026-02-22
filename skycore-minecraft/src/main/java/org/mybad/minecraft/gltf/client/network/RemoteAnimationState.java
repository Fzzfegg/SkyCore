package org.mybad.minecraft.gltf.client.network;

import java.util.UUID;

public final class RemoteAnimationState {
    private static final float PHASE_MIN = 0.0f;
    private static final float PHASE_MAX = 1.0f;

    private final UUID subjectId;
    private final String clipId;
    private final float basePhase;
    private final float speed;
    private final float blendDuration;
    private final boolean loop;
    private final boolean holdLastFrame;
    private final long serverTime;
    private final long clientReceiveTime;

    public RemoteAnimationState(UUID subjectId, String clipId, float phase, float speed,
                                float blendDuration, boolean loop, boolean holdLastFrame, long serverTime,
                                long clientReceiveTime) {
        this.subjectId = subjectId;
        this.clipId = clipId;
        this.basePhase = phase;
        this.speed = speed;
        this.blendDuration = blendDuration;
        this.loop = loop;
        this.holdLastFrame = holdLastFrame;
        this.serverTime = serverTime;
        this.clientReceiveTime = clientReceiveTime;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getClipId() {
        return clipId;
    }

    public float getBlendDuration() {
        return blendDuration;
    }

    public boolean shouldLoop() {
        return loop;
    }
    public boolean shouldHoldLastFrame() { return holdLastFrame; }

    public float phaseAt(long nowMillis) {
        float elapsedSeconds = (nowMillis - clientReceiveTime) * 0.001f;
        float phase = basePhase + speed * elapsedSeconds;
        if (loop) {
            phase = phase - (float) Math.floor(phase);
        } else {
            if (phase < PHASE_MIN) {
                phase = PHASE_MIN;
            }
            if (phase > PHASE_MAX) {
                phase = PHASE_MAX;
            }
            if (holdLastFrame) {
                phase = PHASE_MAX;
            }
        }
        if (phase < PHASE_MIN) {
            return PHASE_MIN;
        }
        if (phase > PHASE_MAX) {
            return PHASE_MAX;
        }
        return phase;
    }

    public float phaseAt(long nowMillis, boolean loopOverride, boolean holdOverride) {
        float elapsedSeconds = (nowMillis - clientReceiveTime) * 0.001f;
        float phase = basePhase + speed * elapsedSeconds;
        if (loopOverride) {
            phase = phase - (float) Math.floor(phase);
        } else {
            if (phase < PHASE_MIN) phase = PHASE_MIN;
            if (phase > PHASE_MAX) phase = PHASE_MAX;
            if (holdOverride) phase = PHASE_MAX;
        }
        if (phase < PHASE_MIN) return PHASE_MIN;
        if (phase > PHASE_MAX) return PHASE_MAX;
        return phase;
    }

    public boolean isExpired(long nowMillis) {
        if (loop || holdLastFrame) {
            return false;
        }
        float elapsedSeconds = (nowMillis - clientReceiveTime) * 0.001f;
        return basePhase + speed * elapsedSeconds >= PHASE_MAX;
    }

    public long getServerTime() {
        return serverTime;
    }

    public long getClientReceiveTime() { return clientReceiveTime; }
}
