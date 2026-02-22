package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

/**
 * Manages {@link CustomEntityInstance}s bound to remote entity appearance profiles.
 */
public final class CustomEntityManager {

    private static final Map<UUID, CustomPlayerConfig> CONFIGS = new HashMap<>();
    private static final Map<UUID, CustomEntityInstance> INSTANCES = new HashMap<>();

    private CustomEntityManager() {
    }

    public static void setEntityConfiguration(UUID entityId, CustomPlayerConfig config) {
        if (entityId == null || config == null) {
            return;
        }
        CONFIGS.put(entityId, config);
        CustomEntityInstance instance = INSTANCES.get(entityId);
        if (instance != null) {
            instance.bindConfiguration(config);
        }
        GltfLog.LOGGER.debug("Registered entity appearance for {}", entityId);
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
        CustomPlayerConfig config = CONFIGS.get(entityId);
        if (config == null) {
            return false;
        }
        CustomEntityInstance instance = INSTANCES.computeIfAbsent(entityId, key -> new CustomEntityInstance());
        if (!instance.isBoundTo(config)) {
            instance.bindConfiguration(config);
        }
        return instance.render(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    public static Vec3d getBoneWorldPosition(EntityLivingBase entity, String boneName, float partialTicks) {
        if (entity == null) {
            return null;
        }
        UUID entityId = entity.getUniqueID();
        CustomPlayerConfig config = CONFIGS.get(entityId);
        if (config == null) {
            return null;
        }
        CustomEntityInstance instance = INSTANCES.computeIfAbsent(entityId, key -> new CustomEntityInstance());
        if (!instance.isBoundTo(config)) {
            instance.bindConfiguration(config);
        }
        if (!instance.isBoundTo(config)) {
            return null;
        }
        return instance.sampleBoneWorldPosition(entity, boneName, partialTicks);
    }
}
