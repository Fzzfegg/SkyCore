package org.mybad.minecraft.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.geometry.GeometryCache;
import org.mybad.minecraft.render.transform.LocatorTransform;

import java.util.List;

public final class BedrockModelHandle {
    private final BedrockModelWrapper wrapper;

    private BedrockModelHandle(BedrockModelWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public static BedrockModelHandle create(Model model,
                                            Animation animation,
                                            ResourceLocation texture,
                                            ResourceLocation emissiveTexture,
                                            ResourceLocation bloomTexture,
                                            ResourceLocation blendTexture,
                                            boolean enableCull,
                                            String modelId,
                                            GeometryCache geometryCache) {
        return new BedrockModelHandle(new BedrockModelWrapper(
            model,
            animation,
            texture,
            emissiveTexture,
            bloomTexture,
            blendTexture,
            enableCull,
            modelId,
            geometryCache
        ));
    }

    public void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
    }

    public void renderBlock(double x, double y, double z, float yaw, float partialTicks) {
        wrapper.renderBlock(x, y, z, yaw, partialTicks);
    }

    public void setAnimation(Animation animation) {
        wrapper.setAnimation(animation);
    }

    public void restartAnimation() {
        wrapper.restartAnimation();
    }

    public AnimationPlayer getActiveAnimationPlayer() {
        return wrapper.getActiveAnimationPlayer();
    }

    public void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        wrapper.setOverlayStates(states);
    }

    public void clearOverlayStates() {
        wrapper.clearOverlayStates();
    }

    public float[] resolveLocatorPosition(String locatorName) {
        return wrapper.resolveLocatorPosition(locatorName);
    }

    public boolean resolveLocatorTransform(String locatorName, LocatorTransform out) {
        return wrapper.resolveLocatorTransform(locatorName, out);
    }

    public void setPrimaryFadeDuration(float seconds) {
        wrapper.setPrimaryFadeDuration(seconds);
    }

    public void setEmissiveStrength(float strength) {
        wrapper.setEmissiveStrength(strength);
    }

    public void setBloomStrength(float strength) {
        wrapper.setBloomStrength(strength);
    }

    public void setBloomColor(int[] color) {
        wrapper.setBloomColor(color);
    }

    public void setBloomPasses(int passes) {
        wrapper.setBloomPasses(passes);
    }

    public void setBloomScaleStep(float step) {
        wrapper.setBloomScaleStep(step);
    }

    public void setBloomDownscale(float downscale) {
        wrapper.setBloomDownscale(downscale);
    }

    public void setBloomOffset(float[] offset) {
        wrapper.setBloomOffset(offset);
    }

    public void setModelScale(float scale) {
        wrapper.setModelScale(scale);
    }

    public void setRenderHurtTint(boolean renderHurtTint) {
        wrapper.setRenderHurtTint(renderHurtTint);
    }

    public void setBlendMode(ModelBlendMode mode) {
        wrapper.setBlendMode(mode);
    }

    public void setBlendColor(float[] color) {
        wrapper.setBlendColor(color);
    }

    public void setHurtTint(float[] hurtTint) {
        wrapper.setHurtTint(hurtTint);
    }

    public float getModelScale() {
        return wrapper.getModelScale();
    }

    public void dispose() {
        wrapper.dispose();
    }

    public void updateAnimations() {
        wrapper.updateAnimations();
    }

}
