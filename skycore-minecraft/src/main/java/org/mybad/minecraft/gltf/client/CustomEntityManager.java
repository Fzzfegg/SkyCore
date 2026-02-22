package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * Manages {@link CustomEntityInstance}s bound to remote entity appearance profiles.
 */
public final class CustomEntityManager {

    private static final Map<UUID, GltfProfile> CONFIGS = new HashMap<>();
    private static final Map<UUID, CustomEntityInstance> INSTANCES = new HashMap<>();

    private CustomEntityManager() {
    }

    public static void setEntityConfiguration(UUID entityId, GltfProfile config) {
        if (entityId == null || config == null) {
            return;
        }
        GltfProfile previous = CONFIGS.get(entityId);
        if (previous == config) {
            return;
        }
        CONFIGS.put(entityId, config);
        CustomEntityInstance instance = INSTANCES.get(entityId);
        if (instance != null) {
            instance.bindConfiguration(config);
        }
        String newProfile = config.getName() != null && !config.getName().isEmpty() ? config.getName() : "<unnamed>";
        if (previous == null) {
            GltfLog.LOGGER.info("Entity {} 绑定 GLTF profile {}", entityId, newProfile);
        } else {
            String oldProfile = previous.getName() != null && !previous.getName().isEmpty() ? previous.getName() : "<unnamed>";
            GltfLog.LOGGER.info("Entity {} 切换 GLTF profile: {} -> {}", entityId, oldProfile, newProfile);
        }
    }

    public static void removeEntity(UUID entityId) {
        if (entityId == null) {
            return;
        }
        CONFIGS.remove(entityId);
        CustomEntityInstance instance = INSTANCES.remove(entityId);
        if (instance != null) {
            instance.unbindModel();
        }
        GltfLog.LOGGER.debug("Removed entity appearance for {}", entityId);
    }

    public static boolean hasConfiguration(UUID entityId) {
        if (entityId == null) {
            return false;
        }
        return CONFIGS.containsKey(entityId);
    }

    public static void pruneMissing(World world) {
        if (world == null) {
            return;
        }
        Iterator<Map.Entry<UUID, GltfProfile>> iterator = CONFIGS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GltfProfile> entry = iterator.next();
            UUID uuid = entry.getKey();
            if (!containsEntityWithUuid(world, uuid)) {
                iterator.remove();
                CustomEntityInstance instance = INSTANCES.remove(uuid);
                if (instance != null) {
                    instance.unbindModel();
                }
                GltfLog.LOGGER.debug("Pruned stale GLTF entity {}", uuid);
            }
        }
    }

    private static boolean containsEntityWithUuid(World world, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Entity entity = world.getPlayerEntityByUUID(uuid);
        if (entity != null) {
            return true;
        }
        for (Entity candidate : world.loadedEntityList) {
            if (uuid.equals(candidate.getUniqueID())) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        CONFIGS.clear();
        INSTANCES.values().forEach(CustomEntityInstance::unbindModel);
        INSTANCES.clear();
        GltfLog.LOGGER.debug("Cleared all custom entity instances");
    }

    public static boolean renderCustomEntity(EntityLivingBase entity, double x, double y, double z,
                                             float entityYaw, float partialTicks) {
        if (entity == null) {
            return false;
        }
        UUID entityId = entity.getUniqueID();
        GltfProfile config = CONFIGS.get(entityId);
        if (config == null) {
            return false;
        }
        CustomEntityInstance instance = INSTANCES.computeIfAbsent(entityId, key -> new CustomEntityInstance());
        if (!instance.isBoundTo(config)) {
            instance.bindConfiguration(config);
        }
        return instance.render(entity, x, y, z, entityYaw, partialTicks);
    }

}
