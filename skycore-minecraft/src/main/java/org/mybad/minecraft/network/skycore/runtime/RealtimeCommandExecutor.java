package org.mybad.minecraft.network.skycore.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.common.indicator.IndicatorRendererEvent;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.render.entity.EntityAttachmentManager;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.mybad.minecraft.render.world.WorldActorManager;
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
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        ResourceCacheManager cacheManager = SkyCoreMod.instance.getResourceCacheManager();
        if (cacheManager == null) {
            return;
        }
        java.util.UUID entityUuid;
        try {
            entityUuid = java.util.UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ex) {
            entityUuid = null;
        }
        if (entityUuid != null) {
            applyForcedAnimation(handler, cacheManager, entityUuid, packet.getClipName());
        } else {
            handleForceAnimationByName(handler, cacheManager, rawUuid, packet.getClipName());
        }
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

    public static void handleForceSkullAnimation(SkyCoreProto.ForceSkullAnimation packet) {
        if (packet == null || packet.getClipName() == null || packet.getClipName().trim().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.world.provider == null) {
            return;
        }
        int dimension = mc.world.provider.getDimension();
        BlockPos pos = new BlockPos(packet.getX(), packet.getY(), packet.getZ());
        SkullModelManager.forceClip(dimension, pos, packet.getClipName(), packet.getOnce());
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

    public static void handleWorldActorCommand(SkyCoreProto.WorldActorCommand packet) {
        if (packet == null) {
            return;
        }
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        WorldActorManager manager = handler.getWorldActorManager();
        if (manager == null) {
            return;
        }
        SkyCoreProto.WorldActorCommand.Action action = packet.getAction();
        switch (action) {
            case SPAWN:
                manager.spawnActor(packet);
                break;
            case REMOVE:
                manager.removeActor(packet.getId());
                break;
            case CLEAR_ALL:
                manager.clear();
                break;
            default:
        }
    }

    private static float[] toArray(java.util.List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static void applyForcedAnimation(EntityRenderEventHandler handler,
                                             ResourceCacheManager cacheManager,
                                             java.util.UUID entityUuid,
                                             String clipName) {
        if (entityUuid == null || clipName == null) {
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
        Animation animation = cacheManager.loadAnimation(mapping.getAnimation(), clipName);
        if (animation == null) {
            return;
        }
        handler.setForcedAnimation(entityUuid, animation);
    }

    private static void handleForceAnimationByName(EntityRenderEventHandler handler,
                                                   ResourceCacheManager cacheManager,
                                                   String rawName,
                                                   String clipName) {
        if (rawName == null) {
            return;
        }
        String target = rawName.trim();
        if (target.isEmpty() || clipName == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return;
        }
        String normalized = target.toLowerCase(java.util.Locale.ROOT);
        for (Entity entity : mc.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            java.util.UUID uuid = living.getUniqueID();
            if (uuid == null) {
                continue;
            }
            String mappingName = handler.resolveMappingByEntity(uuid);
            if (mappingName == null) {
                continue;
            }
            if (!matchesEntityName(living, mappingName, normalized)) {
                continue;
            }
            applyForcedAnimation(handler, cacheManager, uuid, clipName);
        }
    }

    private static boolean matchesEntityName(EntityLivingBase living,
                                             String mappingName,
                                             String normalizedTarget) {
        if (normalizedTarget == null || normalizedTarget.isEmpty()) {
            return false;
        }
        if (living.hasCustomName()) {
            String custom = living.getCustomNameTag();
            if (equalsIgnoreCase(custom, normalizedTarget)) {
                return true;
            }
        }
        String base = living.getName();
        if (equalsIgnoreCase(base, normalizedTarget)) {
            return true;
        }
        if (mappingName != null && equalsIgnoreCase(mappingName, normalizedTarget)) {
            return true;
        }
        return false;
    }

    private static boolean equalsIgnoreCase(String value, String normalizedTarget) {
        if (value == null || normalizedTarget == null) {
            return false;
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT).equals(normalizedTarget);
    }

}
