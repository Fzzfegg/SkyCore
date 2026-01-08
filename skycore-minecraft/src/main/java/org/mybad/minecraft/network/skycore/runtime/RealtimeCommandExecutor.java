package org.mybad.minecraft.network.skycore.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.skycoreproto.SkyCoreProto;

@SideOnly(Side.CLIENT)
public final class RealtimeCommandExecutor {
    private RealtimeCommandExecutor() {}

    public static void handleForceAnimation(SkyCoreProto.ForceAnimation packet) {
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(packet.getMappingName());
        if (mapping == null || mapping.getAnimation() == null) {
            return;
        }
        ResourceCacheManager cacheManager = SkyCoreMod.instance.getResourceCacheManager();
        Animation animation = cacheManager.loadAnimation(mapping.getAnimation(), packet.getClipName());
        if (animation == null) {
            return;
        }
        handler.setForcedAnimation(packet.getMappingName(), animation);
    }

    public static void handleClearAnimation(SkyCoreProto.ClearForceAnimation packet) {
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        if (packet.getScope() == SkyCoreProto.ClearForceAnimation.Scope.ALL) {
            handler.clearAllForcedAnimations();
        } else if (!packet.getMappingName().isEmpty()) {
            handler.clearForcedAnimation(packet.getMappingName());
        }
    }

    public static void handleSetModelAttributes(SkyCoreProto.SetModelAttributes packet) {
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(packet.getMappingName());
        if (mapping == null) {
            return;
        }
        if (packet.hasScale()) {
            mapping.setModelScale(packet.getScale());
        }
        if (packet.hasEmissiveStrength()) {
            mapping.setEmissiveStrength(packet.getEmissiveStrength());
        }
        if (packet.hasBloomStrength()) {
            // reuse emissiveStrength field to simulate bloom adjustments if needed
        }
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler != null) {
            handler.invalidateWrapper(packet.getMappingName());
        }
    }

    public static void handleSpawnParticle(SkyCoreProto.SpawnParticle packet) {
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        system.spawn(packet.getEffect(), packet.getX(), packet.getY(), packet.getZ(), packet.getCount());
    }

    public static void handleClearParticles(SkyCoreProto.ClearParticles packet) {
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        system.clear();
    }
}
