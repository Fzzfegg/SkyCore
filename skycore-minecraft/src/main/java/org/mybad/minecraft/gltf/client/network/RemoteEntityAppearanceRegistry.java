package org.mybad.minecraft.gltf.client.network;

import org.mybad.minecraft.gltf.client.CustomEntityManager;
import org.mybad.minecraft.gltf.client.CustomPlayerConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Registry for managing remote entity appearances
 * Stores entity UUID -> entity type and model configuration mappings
 */
@SideOnly(Side.CLIENT)
public class RemoteEntityAppearanceRegistry {

    private static final Map<UUID, EntityAppearanceInfo> entities = new HashMap<>();

    public static class EntityAppearanceInfo {
        public final UUID entityId;
        public final String entityType;
        public final CustomPlayerConfig config;

        public EntityAppearanceInfo(UUID entityId, String entityType, CustomPlayerConfig config) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.config = config;
        }
    }

    /**
     * Register entity appearance
     */
    public static void registerEntity(UUID entityId, String entityType, CustomPlayerConfig config) {
        entities.put(entityId, new EntityAppearanceInfo(entityId, entityType, config));
        CustomEntityManager.setEntityConfiguration(entityId, config);
    }

    /**
     * Get entity appearance info
     */
    public static EntityAppearanceInfo getEntityAppearance(UUID entityId) {
        return entities.get(entityId);
    }

    /**
     * Check if entity has a custom appearance
     */
    public static boolean hasAppearance(UUID entityId) {
        return entities.containsKey(entityId);
    }

    /**
     * Remove entity appearance
     */
    public static void removeEntity(UUID entityId) {
        entities.remove(entityId);
        CustomEntityManager.removeEntity(entityId);
    }

    /**
     * Clear all entity appearances
     */
    public static void clearAll() {
        entities.clear();
        CustomEntityManager.clear();
    }

    /**
     * Remove entries whose backing entity no longer exists client-side.
     */
    public static void pruneMissing(World world) {
        if (world == null) {
            return;
        }
        Iterator<Map.Entry<UUID, EntityAppearanceInfo>> iterator = entities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EntityAppearanceInfo> entry = iterator.next();
            if (!containsEntityWithUuid(world, entry.getKey())) {
                iterator.remove();
                CustomEntityManager.removeEntity(entry.getKey());
            }
        }
    }

    private static boolean containsEntityWithUuid(World world, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        EntityPlayer player = world.getPlayerEntityByUUID(uuid);
        if (player != null) {
            return true;
        }
        for (net.minecraft.entity.Entity entity : world.loadedEntityList) {
            if (uuid.equals(entity.getUniqueID())) {
                return true;
            }
        }
        return false;
    }
}
