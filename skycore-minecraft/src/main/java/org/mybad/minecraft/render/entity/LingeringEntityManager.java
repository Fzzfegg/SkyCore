package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds onto Bedrock model wrappers after their backing entities are removed
 * so death animations can finish rendering.
 */
public final class LingeringEntityManager {
    private static final class LingeringEntry {
        final BedrockModelHandle wrapper;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final int packedLight;
        int remainingTicks;

        LingeringEntry(BedrockModelHandle wrapper,
                       double x,
                       double y,
                       double z,
                       float yaw,
                       int packedLight,
                       int remainingTicks) {
            this.wrapper = wrapper;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.packedLight = packedLight;
            this.remainingTicks = remainingTicks;
        }
    }

    private final List<LingeringEntry> entries = new ArrayList<>();

    boolean adopt(Entity entity, EntityWrapperEntry entry) {
        if (entry == null || entry.wrapper == null) {
            return false;
        }
        if (!shouldAdopt(entity, entry)) {
            return false;
        }
        if (!ensureDeathAnimation(entry)) {
            return false;
        }
        BedrockModelHandle wrapper = entry.wrapper;
        AnimationPlayer player = wrapper.getActiveAnimationPlayer();
        if (player == null) {
            return false;
        }
        Animation animation = player.getAnimation();
        if (animation == null) {
            return false;
        }
        double x = entry.lastWorldX;
        double y = entry.lastWorldY;
        double z = entry.lastWorldZ;
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return false;
        }
        float yaw = entry.lastBodyYaw;
        int lifetimeTicks = computeRemainingTicks(animation, player);
        if (lifetimeTicks <= 0) {
            return false;
        }
        int packedLight = entry.lastPackedLight;
        if (packedLight == 0 && entity instanceof EntityLivingBase) {
            packedLight = ((EntityLivingBase) entity).getBrightnessForRender();
        }
        entries.add(new LingeringEntry(wrapper, x, y, z, yaw, packedLight, lifetimeTicks));
        entry.trailController.clear();
        return true;
    }

    void tick() {
        if (entries.isEmpty()) {
            return;
        }
        Iterator<LingeringEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            LingeringEntry entry = iterator.next();
            entry.remainingTicks--;
            if (entry.remainingTicks <= 0 || isAnimationFinished(entry.wrapper)) {
                entry.wrapper.dispose();
                iterator.remove();
            }
        }
    }

    void render(float partialTicks) {
        if (entries.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getRenderManager() == null) {
            return;
        }
        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;
        int prevLightX = (int) OpenGlHelper.lastBrightnessX;
        int prevLightY = (int) OpenGlHelper.lastBrightnessY;
        for (LingeringEntry entry : entries) {
            if (shouldAdvanceAnimation(entry.wrapper)) {
                entry.wrapper.updateAnimations();
            }
            applyLight(entry.packedLight);
            entry.wrapper.renderBlock(
                entry.x - cameraX,
                entry.y - cameraY,
                entry.z - cameraZ,
                entry.yaw,
                partialTicks
            );
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
    }

    public void clear() {
        for (LingeringEntry entry : entries) {
            if (entry.wrapper != null) {
                entry.wrapper.dispose();
            }
        }
        entries.clear();
    }

    private int computeRemainingTicks(Animation animation, AnimationPlayer player) {
        float length = Math.max(0f, animation.getLength());
        float currentTime = player.getState().getCurrentTime();
        float remaining = Math.max(0f, length - currentTime);
        float speed = Math.max(0.001f, animation.getSpeed());
        float remainingSeconds = remaining / speed;
        if (remainingSeconds <= 0f) {
            if (!animation.isHoldOnLastFrame()) {
                return 0;
            }
            remainingSeconds = 0.05f;
        }
        return Math.max(1, (int) Math.ceil(remainingSeconds * 20f));
    }

    private boolean isAnimationFinished(BedrockModelHandle wrapper) {
        AnimationPlayer player = wrapper.getActiveAnimationPlayer();
        if (player == null) {
            return true;
        }
        if (!player.isFinished()) {
            return false;
        }
        Animation animation = player.getAnimation();
        if (animation == null) {
            return true;
        }
        if (animation.isHoldOnLastFrame()) {
            return player.isFinished();
        }
        return true;
    }

    private boolean shouldAdopt(Entity entity, EntityWrapperEntry entry) {
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            return living.getHealth() <= 0f || living.deathTime > 0;
        }
        return entry.lastKnownDead;
    }

    private boolean ensureDeathAnimation(EntityWrapperEntry entry) {
        if (entry == null || entry.wrapper == null) {
            return false;
        }
        AnimationPlayer player = entry.wrapper.getActiveAnimationPlayer();
        Animation current = player != null ? player.getAnimation() : null;
        if (isDeathNamed(current)) {
            return true;
        }
        Animation death = resolveDeathAnimation(entry);
        if (death == null) {
            return false;
        }
        entry.wrapper.setAnimation(death);
        entry.wrapper.clearOverlayStates();
        entry.wrapper.restartAnimation();
        entry.lastKnownDead = true;
        return true;
    }

    private boolean shouldAdvanceAnimation(BedrockModelHandle wrapper) {
        if (wrapper == null) {
            return false;
        }
        AnimationPlayer player = wrapper.getActiveAnimationPlayer();
        if (player == null) {
            return false;
        }
        Animation animation = player.getAnimation();
        if (animation == null) {
            return false;
        }
        if (player.isFinished() && !animation.isHoldOnLastFrame()) {
            return false;
        }
        return true;
    }

    private void applyLight(int packedLight) {
        if (packedLight <= 0) {
            return;
        }
        int lightX = packedLight & 0xFFFF;
        int lightY = (packedLight >> 16) & 0xFFFF;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
    }

    private Animation resolveDeathAnimation(EntityWrapperEntry entry) {
        EntityAnimationController controller = entry.controller;
        if (controller == null) {
            return null;
        }
        Animation clip = controller.getAction("dying");
        if (clip == null) {
            clip = controller.getAction("death");
        }
        if (clip == null) {
            clip = controller.getAction("die");
        }
        return clip;
    }

    private boolean isDeathNamed(Animation animation) {
        if (animation == null || animation.getName() == null) {
            return false;
        }
        String name = animation.getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("death") || name.contains("dying") || name.contains("die");
    }
}
