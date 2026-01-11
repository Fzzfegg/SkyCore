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
        String rawUuid = packet.getEntityUuid();
        if (rawUuid == null || rawUuid.isEmpty()) {
            return;
        }
        java.util.UUID entityUuid;
        try {
            entityUuid = java.util.UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ex) {
            return;
        }
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        String mappingName = handler.resolveMappingByEntity(entityUuid);
        if (mappingName == null) {
            return;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null || mapping.getAnimation() == null) {
            return;
        }
        ResourceCacheManager cacheManager = SkyCoreMod.instance.getResourceCacheManager();
        Animation animation = cacheManager.loadAnimation(mapping.getAnimation(), packet.getClipName());
        if (animation == null) {
            return;
        }
        handler.setForcedAnimation(entityUuid, animation);
    }

    public static void handleSetModelAttributes(SkyCoreProto.SetModelAttributes packet) {
        String mappingName = packet.hasMappingName() ? packet.getMappingName() : null;
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (mappingName == null && packet.hasEntityUuid() && handler != null) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(packet.getEntityUuid());
                mappingName = handler.resolveMappingByEntity(uuid);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (mappingName == null) {
            return;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
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
        if (packet.hasTexture()) {
            mapping.setTexture(packet.getTexture());
        }
        if (packet.hasEmissive()) {
            mapping.setEmissive(packet.getEmissive());
        }
        if (packet.hasBloom()) {
            mapping.setBloom(packet.getBloom());
        }
        if (packet.hasBlendTexture()) {
            mapping.setBlendTexture(packet.getBlendTexture());
        }
        if (packet.hasEnableCull()) {
            mapping.setEnableCull(packet.getEnableCull());
        }
        if (packet.hasRenderShadow()) {
            mapping.setRenderShadow(packet.getRenderShadow());
        }
        if (packet.hasPrimaryFadeSeconds()) {
            mapping.setPrimaryFadeSeconds(packet.getPrimaryFadeSeconds());
        }
        if (handler != null) {
            handler.invalidateWrapper(mappingName);
        }
    }

    public static void handleSpawnParticle(SkyCoreProto.SpawnParticle packet) {
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        system.spawn(packet.getEffect(), packet.getX(), packet.getY(), packet.getZ(), packet.getCount());
    }

}
