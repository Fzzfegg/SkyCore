package gg.moonflower.pollen.particle.render;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.pinwheel.particle.render.Flipbook;
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
        int frame;
        if (flipbook.stretchToLifetime()) {
            frame = Math.min((int) (time / maxLife * (maxFrame + 1)), maxFrame);
        } else {
            frame = (int) (time * flipbook.fps());
            if (flipbook.loop()) {
                frame = maxFrame > 0 ? frame % maxFrame : 0;
            } else {
                frame = Math.min(frame, maxFrame);
            }
        }

        float u = environment.safeResolve(flipbook.baseU());
        float v = environment.safeResolve(flipbook.baseV());
        float uSize = flipbook.sizeU();
        float vSize = flipbook.sizeV();
        float uo = flipbook.stepU() * frame;
        float vo = flipbook.stepV() * frame;

        float uMin = (u + uo) / (float) textureWidth;
        float vMin = (v + vo) / (float) textureHeight;
        float uMax = (u + uo + uSize) / (float) textureWidth;
        float vMax = (v + vo + vSize) / (float) textureHeight;
        this.setUV(uMin, vMin, uMax, vMax);
    }
}
