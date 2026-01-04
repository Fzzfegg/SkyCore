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
                                            boolean enableCull,
                                            String modelId,
                                            GeometryCache geometryCache) {
        return new BedrockModelHandle(new BedrockModelWrapper(
            model,
            animation,
            texture,
            emissiveTexture,
            bloomTexture,
            enableCull,
            modelId,
            geometryCache
        ));
    }

    public void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
    }

    public void setAnimation(Animation animation) {
        wrapper.setAnimation(animation);
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

    public void setModelScale(float scale) {
        wrapper.setModelScale(scale);
    }

    public float getModelScale() {
        return wrapper.getModelScale();
    }

    public void dispose() {
        wrapper.dispose();
    }
}
