package org.mybad.minecraft.network.skycore.runtime;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.common.indicator.IndicatorRendererEvent;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.render.entity.EntityAttachmentManager;
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
        if (mappingName != null) {
            EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
            if (mapping != null) {
                if (packet.hasScale()) {
                    mapping.setModelScale(packet.getScale());
                }
                if (packet.hasEmissiveStrength()) {
                    mapping.setEmissiveStrength(packet.getEmissiveStrength());
                }
                if (packet.hasBloomStrength()) {
                    mapping.setBloomStrength(packet.getBloomStrength());
                }
                if (packet.hasBloomPasses()) {
                    mapping.setBloomPasses(packet.getBloomPasses());
                }
                if (packet.hasBloomScaleStep()) {
                    mapping.setBloomScaleStep(packet.getBloomScaleStep());
                }
                if (packet.hasBloomDownscale()) {
                    mapping.setBloomDownscale(packet.getBloomDownscale());
                }
                if (packet.getBloomOffsetCount() >= 3) {
                    mapping.setBloomOffset(toArray(packet.getBloomOffsetList()));
                }
                SkyCoreMod.LOGGER.info("[SkyCore] realtime bloom params for '{}' -> strength={}, passes={}, scaleStep={}, downscale={}",
                    mappingName,
                    mapping.getBloomStrength(),
                    mapping.getBloomPasses(),
                    mapping.getBloomScaleStep(),
                    mapping.getBloomDownscale());
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
        }
        if (handler != null && packet.getOverridesCount() > 0) {
            try {
                handler.applyAttributeOverrides(packet.getOverridesList());
            } catch (Exception ex) {
                SkyCoreMod.LOGGER.error("[SkyCore] Failed to apply attribute overrides", ex);
            }
        }
    }

    public static void handleSpawnParticle(SkyCoreProto.SpawnParticle packet) {
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        system.spawn(packet.getEffect(), packet.getX(), packet.getY(), packet.getZ(), packet.getCount());
    }

    public static void handleEntityAttachment(SkyCoreProto.EntityAttachment packet) {
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null || packet == null) {
            return;
        }
        EntityAttachmentManager manager = handler.getAttachmentManager();
        if (manager == null) {
            return;
        }
        manager.spawnAttachment(packet);
    }

    public static void handleRemoveAttachment(SkyCoreProto.RemoveEntityAttachment packet) {
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null || packet == null) {
            return;
        }
        EntityAttachmentManager manager = handler.getAttachmentManager();
        if (manager == null) {
            return;
        }
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(packet.getTargetEntityUuid());
        } catch (IllegalArgumentException ex) {
            return;
        }
        manager.removeAttachment(uuid, packet.getAttachmentId());
    }

    public static void handleIndicatorCommand(SkyCoreProto.IndicatorCommand packet) {
        if (packet == null) {
            return;
        }
        IndicatorRendererEvent.applyIndicatorCommand(packet);
    }

    private static float[] toArray(java.util.List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

}
