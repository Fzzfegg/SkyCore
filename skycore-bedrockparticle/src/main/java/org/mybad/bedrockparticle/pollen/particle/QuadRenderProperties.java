package org.mybad.bedrockparticle.pollen.particle;

import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironment;
import org.mybad.bedrockparticle.pinwheel.particle.Flipbook;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * Minimal QuadRenderProperties (no Minecraft dependency).
 */
public class QuadRenderProperties {

    private float red = 1F;
    private float green = 1F;
    private float blue = 1F;
    private float alpha = 1F;

    private final Quaternionf rotation = new Quaternionf();

    private float width = 0F;
    private float height = 0F;

    private float uMin = 0F;
    private float vMin = 0F;
    private float uMax = 0F;
    private float vMax = 0F;

    private boolean direction = false;
    private int packedLight = 0;

    public float getRed() {
        return red;
    }

    public void setRed(float red) {
        this.red = red;
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(float green) {
        this.green = green;
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(float blue) {
        this.blue = blue;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public void setRotation(Quaternionfc rotation) {
        this.rotation.set(rotation);
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getUMin() {
        return uMin;
    }

    public float getVMin() {
        return vMin;
    }

    public float getUMax() {
        return uMax;
    }

    public float getVMax() {
        return vMax;
    }

    public boolean isDirection() {
        return direction;
    }

    public void setDirection(boolean direction) {
        this.direction = direction;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public void setPackedLight(int packedLight) {
        this.packedLight = packedLight;
    }

    public void setColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void setColor(int color) {
        this.red = (float) (color >> 16 & 0xFF) / 255F;
        this.green = (float) (color >> 8 & 0xFF) / 255F;
        this.blue = (float) (color & 0xFF) / 255F;
        this.alpha = (float) (color >> 24 & 0xFF) / 255F;
    }

    public boolean canRender() {
        return this.width * this.height > 0;
    }

    public void setUV(float uMin, float vMin, float uMax, float vMax) {
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;
    }

    public void setUV(MolangEnvironment environment, int textureWidth, int textureHeight, Flipbook flipbook, float time, float maxLife) {
        int maxFrame = (int) environment.safeResolve(flipbook.maxFrame());
        int frameCount = Math.max(1, maxFrame);
        int lastFrame = frameCount - 1;
        float u = environment.safeResolve(flipbook.baseU());
        float v = environment.safeResolve(flipbook.baseV());
        float uSize = flipbook.sizeU();
        float vSize = flipbook.sizeV();
        float stepU = flipbook.stepU();
        float stepV = flipbook.stepV();
        int minFrame = 0;
        int maxFrameAllowed = lastFrame;
        if (textureWidth > 0 && textureHeight > 0) {
            int[] uBounds = computeFrameBounds(u, uSize, stepU, textureWidth);
            if (uBounds != null) {
                minFrame = Math.max(minFrame, uBounds[0]);
                maxFrameAllowed = Math.min(maxFrameAllowed, uBounds[1]);
            }
            int[] vBounds = computeFrameBounds(v, vSize, stepV, textureHeight);
            if (vBounds != null) {
                minFrame = Math.max(minFrame, vBounds[0]);
                maxFrameAllowed = Math.min(maxFrameAllowed, vBounds[1]);
            }
        }
        int frame;
        if (flipbook.stretchToLifetime()) {
            if (maxLife <= 0.0f) {
                frame = 0;
            } else {
                frame = (int) (time / maxLife * frameCount);
                frame = Math.min(Math.max(frame, 0), lastFrame);
            }
        } else {
            frame = (int) (time * flipbook.fps());
            if (flipbook.loop()) {
                frame = frame % frameCount;
                if (frame < 0) {
                    frame += frameCount;
                }
            } else {
                frame = Math.min(frame, lastFrame);
            }
        }
        if (maxFrameAllowed < minFrame) {
            frame = 0;
        } else {
            frame = Math.min(Math.max(frame, minFrame), maxFrameAllowed);
        }

        float uo = stepU * frame;
        float vo = stepV * frame;

        float uMin = (u + uo) / (float) textureWidth;
        float vMin = (v + vo) / (float) textureHeight;
        float uMax = (u + uo + uSize) / (float) textureWidth;
        float vMax = (v + vo + vSize) / (float) textureHeight;
        this.setUV(uMin, vMin, uMax, vMax);
    }

    private static int[] computeFrameBounds(float base, float size, float step, int textureSize) {
        if (step == 0.0f) {
            return null;
        }
        float boundA = (0.0f - base) / step;
        float boundB = (textureSize - size - base) / step;
        float minBound = Math.min(boundA, boundB);
        float maxBound = Math.max(boundA, boundB);
        int minFrame = (int) Math.ceil(minBound);
        int maxFrame = (int) Math.floor(maxBound);
        return new int[]{minFrame, maxFrame};
    }
}
