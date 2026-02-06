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
import java.util.function.Consumer;

public final class SkullModelManager {
    private static final Map<SkullModelKey, SkullModelInstance> INSTANCES = new ConcurrentHashMap<>();
    private static final Map<SkullModelKey, PendingForcedClip> PENDING_FORCED = new ConcurrentHashMap<>();
    private static final AnimationEventDispatcher EVENT_DISPATCHER = new AnimationEventDispatcher();
    private static final long PENDING_FORCED_EXPIRE_MS = 15000L;
    private static long renderFrameCounter = 0L;
    private static long currentRenderFrameId = 0L;
    private static boolean renderFrameActive = false;

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
        applyPendingForcedClip(key, instance);
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
        instance.markRenderFrame(currentRenderFrameId);
        return true;
    }

    public static void clear() {
        for (SkullModelInstance instance : INSTANCES.values()) {
            instance.dispose();
        }
        INSTANCES.clear();
        PENDING_FORCED.clear();
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
        PENDING_FORCED.remove(key);
        disposeAndRemove(key);
    }

    public static boolean forceClip(int dimension, BlockPos pos, String clipName, boolean once) {
        if (pos == null || clipName == null || clipName.trim().isEmpty()) {
            return false;
        }
        SkullModelKey key = new SkullModelKey(dimension, pos);
        SkullModelInstance instance = INSTANCES.get(key);
        if (instance != null) {
            return instance.forceClip(clipName, once);
        }
        PENDING_FORCED.put(key, new PendingForcedClip(clipName, once, System.currentTimeMillis()));
        return true;
    }

    private static void disposeAndRemove(SkullModelKey key) {
        SkullModelInstance removed = INSTANCES.remove(key);
        if (removed != null) {
            removed.dispose();
        }
    }

    private static void applyPendingForcedClip(SkullModelKey key, SkullModelInstance instance) {
        if (key == null || instance == null) {
            return;
        }
        PendingForcedClip pending = PENDING_FORCED.get(key);
        if (pending == null) {
            return;
        }
        long ageMs = System.currentTimeMillis() - pending.createdAtMs;
        if (ageMs > PENDING_FORCED_EXPIRE_MS) {
            PENDING_FORCED.remove(key);
            return;
        }
        if (instance.forceClip(pending.clipName, pending.once)) {
            PENDING_FORCED.remove(key);
        }
    }

    private static ResourceCacheManager getCacheManager() {
        if (SkyCoreMod.instance == null) {
            return null;
        }
        return SkyCoreMod.instance.getResourceCacheManager();
    }

    public static void collectDebugInfo(Consumer<DebugInfo> consumer) {
        if (consumer == null || INSTANCES.isEmpty()) {
            return;
        }
        for (SkullModelInstance instance : INSTANCES.values()) {
            if (instance == null) {
                continue;
            }
            DebugInfo info = instance.toDebugInfo();
            if (info != null) {
                consumer.accept(info);
            }
        }
    }

    public static void beginRenderFrame() {
        if (renderFrameActive) {
            return;
        }
        renderFrameActive = true;
        currentRenderFrameId = ++renderFrameCounter;
    }

    public static void finishRenderFrame() {
        if (!renderFrameActive) {
            return;
        }
        renderFrameActive = false;
        final long frameId = currentRenderFrameId;
        if (INSTANCES.isEmpty()) {
            return;
        }
        for (SkullModelInstance instance : INSTANCES.values()) {
            if (instance == null) {
                continue;
            }
            if (!instance.renderedThisFrame(frameId)) {
                instance.updateAnimationsFrame();
                instance.markRenderFrame(frameId);
            }
        }
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

    private static final class PendingForcedClip {
        private final String clipName;
        private final boolean once;
        private final long createdAtMs;

        private PendingForcedClip(String clipName, boolean once, long createdAtMs) {
            this.clipName = clipName;
            this.once = once;
            this.createdAtMs = createdAtMs;
        }
    }

    public static final class DebugInfo {
        private final String mappingName;
        private final double worldX;
        private final double worldY;
        private final double worldZ;
        private final float scale;
        private final float yaw;

        DebugInfo(String mappingName, double worldX, double worldY, double worldZ, float scale, float yaw) {
            this.mappingName = mappingName;
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.scale = scale;
            this.yaw = yaw;
        }

        public String getMappingName() {
            return mappingName;
        }

        public double getWorldX() {
            return worldX;
        }

        public double getWorldY() {
            return worldY;
        }

        public double getWorldZ() {
            return worldZ;
        }

        public float getScale() {
            return scale;
        }

        public float getYaw() {
            return yaw;
        }
    }
}
