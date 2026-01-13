package org.mybad.minecraft.render.skull;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.resource.ResourceCacheManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkullModelManager {
    private static final Map<SkullModelKey, SkullModelInstance> INSTANCES = new ConcurrentHashMap<>();
    private static final AnimationEventDispatcher EVENT_DISPATCHER = new AnimationEventDispatcher();

    private SkullModelManager() {
    }

    public static boolean render(TileEntitySkull skull,
                                 double x,
                                 double y,
                                 double z,
                                 float partialTicks) {
        if (skull == null) {
            return false;
        }
        World world = skull.getWorld();
        if (world == null || world.provider == null) {
            return false;
        }
        GameProfile profile = skull.getPlayerProfile();
        if (profile == null) {
            return false;
        }
        BlockPos pos = skull.getPos();
        int dimension = world.provider.getDimension();
        SkullModelKey key = new SkullModelKey(dimension, pos);
        SkullProfileData profileData = SkullProfileData.from(profile);
        if (profileData == null || profileData.getMappingName() == null) {
            disposeAndRemove(key);
            return false;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(profileData.getMappingName());
        if (mapping == null) {
            disposeAndRemove(key);
            return false;
        }
        ResourceCacheManager cacheManager = getCacheManager();
        if (cacheManager == null) {
            return false;
        }
        long tick = world.getTotalWorldTime();
        SkullModelInstance instance = INSTANCES.get(key);
        if (instance == null || !profileData.getMappingName().equals(instance.getMappingName())) {
            if (instance != null) {
                instance.dispose();
            }
            instance = SkullModelInstance.create(cacheManager, mapping, profileData.getMappingName(), tick);
            if (instance == null) {
                return false;
            }
            INSTANCES.put(key, instance);
        }
        instance.markSeen(tick);
        instance.applyProfile(profileData);

        Transform transform = computeTransform(world, pos, skull);
        double renderX = x + transform.offsetX;
        double renderY = y + transform.offsetY;
        double renderZ = z + transform.offsetZ;
        double worldX = pos != null ? pos.getX() + transform.offsetX : transform.offsetX;
        double worldY = pos != null ? pos.getY() + transform.offsetY : transform.offsetY;
        double worldZ = pos != null ? pos.getZ() + transform.offsetZ : transform.offsetZ;
        instance.render(renderX, renderY, renderZ, worldX, worldY, worldZ, transform.yaw, partialTicks);
        instance.dispatchEvents(EVENT_DISPATCHER, partialTicks);
        return true;
    }

    public static void clear() {
        for (SkullModelInstance instance : INSTANCES.values()) {
            instance.dispose();
        }
        INSTANCES.clear();
    }

    public static void remove(TileEntitySkull skull) {
        if (skull == null) {
            return;
        }
        World world = skull.getWorld();
        if (world == null || world.provider == null) {
            return;
        }
        SkullModelKey key = new SkullModelKey(world.provider.getDimension(), skull.getPos());
        disposeAndRemove(key);
    }

    public static boolean isGlobalRenderer(TileEntitySkull skull) {
        if (skull == null) {
            return false;
        }
        SkullProfileData profile = SkullProfileData.from(skull.getPlayerProfile());
        return profile != null && Boolean.TRUE.equals(profile.getGlobalRender());
    }

    private static void disposeAndRemove(SkullModelKey key) {
        SkullModelInstance removed = INSTANCES.remove(key);
        if (removed != null) {
            removed.dispose();
        }
    }

    private static ResourceCacheManager getCacheManager() {
        if (SkyCoreMod.instance == null) {
            return null;
        }
        return SkyCoreMod.instance.getResourceCacheManager();
    }

    private static Transform computeTransform(World world, BlockPos pos, TileEntitySkull skull) {
        Transform transform = new Transform();
        transform.offsetX = 0.5;
        transform.offsetY = 0.0;
        transform.offsetZ = 0.5;
        transform.yaw = 0.0f;

        if (world == null || pos == null) {
            transform.yaw = skull.getSkullRotation() * 360.0f / 16.0f;
            return transform;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        EnumFacing facing = EnumFacing.UP;
        if (block == Blocks.SKULL && state.getProperties().containsKey(BlockSkull.FACING)) {
            facing = state.getValue(BlockSkull.FACING);
        }

        if (facing == EnumFacing.UP) {
            transform.yaw = skull.getSkullRotation() * 360.0f / 16.0f;
        } else {
            transform.offsetY = 0.25;
            int offsetX = facing.getDirectionVec().getX();
            int offsetZ = facing.getDirectionVec().getZ();
            transform.offsetX = 0.5 - offsetX * 0.25;
            transform.offsetZ = 0.5 - offsetZ * 0.25;
            EnumFacing front = facing.getOpposite();
            transform.yaw = front.getHorizontalAngle();
        }
        return transform;
    }

    private static final class Transform {
        double offsetX;
        double offsetY;
        double offsetZ;
        float yaw;
    }

    private static final class SkullModelKey {
        private final int dimension;
        private final long pos;

        private SkullModelKey(int dimension, BlockPos blockPos) {
            this.dimension = dimension;
            this.pos = blockPos != null ? blockPos.toLong() : 0L;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SkullModelKey)) {
                return false;
            }
            SkullModelKey other = (SkullModelKey) obj;
            return this.dimension == other.dimension && this.pos == other.pos;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(dimension);
            return 31 * result + Long.hashCode(pos);
        }
    }
}
