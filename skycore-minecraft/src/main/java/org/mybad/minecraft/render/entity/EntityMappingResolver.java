package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;

public final class EntityMappingResolver {
    private EntityMappingResolver() {
    }

    public static MappingResult resolve(EntityLivingBase entity) {
        if (entity == null || !SkyCoreConfig.isInitialized()) {
            return null;
        }
        String mappingName = getEntityCustomName(entity);
        EntityModelMapping mapping = null;
        if (mappingName != null && !mappingName.isEmpty()) {
            mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        }
        if (mapping == null && entity instanceof EntityPlayer) {
            String playerName = entity.getName();
            mapping = SkyCoreConfig.getInstance().getMapping(playerName);
            if (mapping != null) {
                mappingName = playerName;
            } else {
                mapping = SkyCoreConfig.getInstance().getMapping("player");
                if (mapping != null) {
                    mappingName = "player";
                }
            }
        }
        if (mapping == null) {
            return null;
        }
        return new MappingResult(mappingName, mapping);
    }

    private static String getEntityCustomName(EntityLivingBase entity) {
        if (entity == null || !entity.hasCustomName()) {
            return null;
        }
        return entity.getCustomNameTag();
    }

    private static boolean shouldSkip(EntityLivingBase entity) {
        return false;
    }

    public static final class MappingResult {
        public final String mappingName;
        public final EntityModelMapping mapping;

        private MappingResult(String mappingName, EntityModelMapping mapping) {
            this.mappingName = mappingName;
            this.mapping = mapping;
        }
    }
}
